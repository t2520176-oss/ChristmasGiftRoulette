"""End-to-end generation pipeline: TEXT -> cloud GPU -> GLB -> clean-up -> colours -> 3MF."""
from __future__ import annotations

import shutil
import time
from dataclasses import dataclass, field
from typing import Callable

from .. import i18n, paths
from ..errors import Cancelled, FigureCraftError, ProviderError
from ..logging_setup import get_logger
from ..providers.base import GenerationProvider
from ..settings import Settings
from . import demo, mesh_inspect, mesh_io, mesh_print, prompt_analysis
from .models import CloudStatus, GenerationRequest, JobStatus, Stage, StageState
from .project_store import Project, ProjectStore
from .workspace import Workspace

log = get_logger("pipeline")


@dataclass
class Hooks:
    stage: Callable[[Stage, StageState, str], None] = lambda s, st, d: None
    cloud: Callable[[CloudStatus], None] = lambda s: None
    info: Callable[[str], None] = lambda t: None
    cancelled: Callable[[], bool] = lambda: False
    hints: Callable[[prompt_analysis.PromptHints], None] = lambda h: None
    gpu_warning: Callable[[str], None] = lambda t: None


@dataclass
class PipelineResult:
    project: Project
    workspace: Workspace
    started: float
    finished: float
    gpu_seconds_est: float | None
    gpu_stop: str = "n/a"          # n/a | ok | failed | manual
    notes: list[str] = field(default_factory=list)


def _check_cancel(h: Hooks) -> None:
    if h.cancelled():
        raise Cancelled()


def run_generation(request: GenerationRequest, provider: GenerationProvider, settings: Settings,
                   store: ProjectStore, hooks: Hooks, project: Project | None = None) -> PipelineResult:
    """Run all 11 stages. `project` given -> add a new version to it (revision / regenerate)."""
    t_start = time.time()
    uses_gpu = not provider.capabilities().is_demo      # lifecycle / billing logic
    is_demo = not uses_gpu                               # labelling (also set when the server runs its mock engine)
    revision = project is not None
    gpu_started = False
    gpu_stop = "n/a"
    t_gpu0: float | None = None
    t_gpu1: float | None = None
    gpu_seconds_reported: float | None = None
    cur: Stage | None = None

    def begin(s: Stage, detail: str = "") -> None:
        nonlocal cur
        cur = s
        hooks.stage(s, StageState.ACTIVE, detail)
        _check_cancel(hooks)

    def done(s: Stage, detail: str = "", state: StageState = StageState.DONE) -> None:
        hooks.stage(s, state, detail)

    try:
        # ------------------------------------------------ 1 prompt analysis
        begin(Stage.PROMPT)
        if not request.prompt.strip():
            raise FigureCraftError("generation_failed", "empty prompt")
        h = prompt_analysis.analyze(request.prompt)
        if h.height_mm:
            request.print.height_mm = h.height_mm
        if h.max_colors:
            request.max_colors = h.max_colors
        if h.flat_bottom:
            request.print.flat_bottom = True
        if h.min_wall_mm:
            request.print.wall_mm = h.min_wall_mm
        hooks.hints(h)
        if request.mode == "image_text" and request.reference_image and not provider.capabilities().image \
                and not provider.capabilities().text_image:
            raise ProviderError("image_unsupported")
        if project is None:
            title = request.prompt.strip().splitlines()[0][:40] or "Figure"
            project = store.create(title=("DEMO " if is_demo else "") + title, prompt=request.prompt, is_demo=is_demo)
        project.meta.is_demo = is_demo
        project.meta.status = "running"
        project.meta.provider, project.meta.engine = provider.id, request.engine
        project.meta.request = request.to_dict()
        project.save()
        done(Stage.PROMPT, f"{request.print.height_mm:.0f} mm, {request.max_colors}c")

        # ------------------------------------------------ 2 connect
        begin(Stage.CONNECT)
        hooks.cloud(CloudStatus.CONNECTING)
        info = provider.test_connection()
        if not info.ok:
            raise ProviderError("server_unavailable", info.message)
        if info.engine == "mock" and not is_demo:         # server-side placeholder engine: never pass it off as AI
            is_demo = True
            project.meta.is_demo = True
            if not project.meta.title.startswith("DEMO"):
                project.meta.title = "DEMO " + project.meta.title
            project.save()
        if provider.supports_lifecycle and uses_gpu:
            provider.start_gpu(lambda m: hooks.info(m))
            gpu_started = True
        hooks.cloud(CloudStatus.READY)
        done(Stage.CONNECT, "DEMO" if is_demo else info.engine)

        # ------------------------------------------------ 3 generate
        begin(Stage.GENERATE)
        hooks.cloud(CloudStatus.GENERATING)
        t_gpu0 = time.time()
        job_id = provider.submit(request)
        project.meta.last_job_id = job_id

        def on_status(st: JobStatus) -> None:
            hooks.info((st.stage or st.state) + (f" {st.progress * 100:.0f}%" if st.progress is not None else ""))
        final = provider.wait(job_id, hooks.cancelled, on_status, interval=0.5 if is_demo else 3.0)
        t_gpu1 = time.time()
        gpu_seconds_reported = final.gpu_seconds
        if final.state == "failed":
            raise ProviderError("generation_failed", final.error or final.message)
        if final.state == "cancelled":
            raise Cancelled()
        done(Stage.GENERATE)

        # ------------------------------------------------ 4 download
        begin(Stage.DOWNLOAD)
        from . import export as _export
        _export.check_disk_space(project.root, min(settings.max_download_mb, 500) * 1.5)   # fail early, in Korean/English
        mesh_dir_tmp = paths.temp_dir() / f"dl_{project.meta.id}.glb"
        glb = provider.download(job_id, mesh_dir_tmp, lambda a, b: hooks.info(f"{a / 1e6:.1f} MB"))
        shutil.move(str(glb), project.file("original.glb"))
        done(Stage.DOWNLOAD, f"{project.file('original.glb').stat().st_size / 1e6:.1f} MB")
        # Cost saving: stop the GPU as soon as the file is here, before any local processing.
        if provider.supports_lifecycle and uses_gpu:
            if settings.auto_stop_gpu:
                gpu_stop = _stop_gpu(provider, hooks, True)
                gpu_started = gpu_stop != "ok" and gpu_started
            else:
                gpu_stop = "manual"
                hooks.gpu_warning(provider.manual_instructions(i18n.get_language()))
        elif uses_gpu:
            gpu_stop = "manual"
            hooks.cloud(CloudStatus.READY)
        else:
            hooks.cloud(CloudStatus.READY)

        # ------------------------------------------------ 5 validate
        begin(Stage.VALIDATE)
        ws = Workspace(project, settings)
        ws.request = request
        ws.hints = h.colors
        raw = mesh_io.load_mesh(project.file("original.glb"))
        mesh_print.fit_to_height(raw, request.print.height_mm)
        ws.mesh = raw
        before = mesh_inspect.inspect_mesh(raw, request.print, with_thin=False)
        if before.status == mesh_inspect.FAILED:
            raise FigureCraftError("invalid_mesh", [i.code for i in before.issues])
        done(Stage.VALIDATE, before.status)

        # ------------------------------------------------ 6 repair
        begin(Stage.REPAIR)
        actions = ws.repair()
        done(Stage.REPAIR, f"{len(actions)}")

        # ------------------------------------------------ 7 print optimisation
        begin(Stage.OPTIMIZE)
        o = request.print
        if o.flat_bottom:
            ws.apply_flat_bottom()
        if o.add_base and o.base_shape != "none":
            ws.add_base()
        if o.strengthen_thin_parts and o.min_thickness_protection and ws.report and ws.report.watertight:
            ws.strengthen()
        mesh_print.fit_to_height(ws.mesh, o.height_mm) if not o.add_base else mesh_print.ground(ws.mesh)
        ws.inspect(with_thin=True)
        done(Stage.OPTIMIZE, ws.report.status)

        # ------------------------------------------------ 8/9 colours
        begin(Stage.COLOR_ANALYSIS)
        import numpy as np
        n_src = len(np.unique(ws.orig_colors >> 3, axis=0))
        done(Stage.COLOR_ANALYSIS, f"{n_src}")
        begin(Stage.COLOR_REDUCE)
        ws.reduce(request.max_colors, request.palette_mode, request.custom_palette)
        done(Stage.COLOR_REDUCE, ", ".join(p.name for p in ws.palette))

        # ------------------------------------------------ 10 export
        begin(Stage.EXPORT)
        ws.project.meta.timing = {
            "started": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(t_start)),
            "gpu_seconds": gpu_seconds_reported if gpu_seconds_reported is not None else
            ((t_gpu1 - t_gpu0) if (t_gpu0 and t_gpu1 and uses_gpu) else 0.0),
            "gpu_seconds_source": "reported" if gpu_seconds_reported is not None else ("wall-clock" if uses_gpu else "none"),
        }
        ws.save(rebuild_exports=True, mode=settings.default_3mf_mode)
        done(Stage.EXPORT, ws.project.meta.timing.get("gpu_seconds") and f"{ws.project.meta.timing['gpu_seconds']:.0f}s" or "")
        note = ("revision: " + request.revision_instruction) if request.revision_instruction else "initial"
        project.meta.status = "ready"
        project.snapshot_version(note=note)
        project.meta.timing["finished"] = time.strftime("%Y-%m-%d %H:%M:%S")
        project.save()

        # ------------------------------------------------ 11 ready
        begin(Stage.READY)
        done(Stage.READY)
        t_end = time.time()
        return PipelineResult(project, ws, t_start, t_end, project.meta.timing.get("gpu_seconds"), gpu_stop)

    except BaseException as exc:
        if cur is not None:
            hooks.stage(cur, StageState.FAILED, str(exc)[:200])
        if project is not None:
            project.meta.status = "failed" if not isinstance(exc, Cancelled) else (
                "ready" if project.meta.versions else "failed")
            try:
                project.save()
            except Exception:
                pass
        if gpu_started and uses_gpu:              # never leave a paid GPU running after a failure / cancel
            hooks.cloud(CloudStatus.STOPPING)
            _stop_gpu(provider, hooks, settings.auto_stop_gpu)
        hooks.cloud(CloudStatus.DISCONNECTED if uses_gpu else CloudStatus.READY)
        log.error("pipeline aborted at stage %s: %s", cur, exc)
        raise


def _stop_gpu(provider: GenerationProvider, hooks: Hooks, auto: bool) -> str:
    if not auto:
        hooks.gpu_warning(provider.manual_instructions(i18n.get_language()))
        return "manual"
    hooks.cloud(CloudStatus.STOPPING)
    try:
        provider.stop_gpu()
        hooks.cloud(CloudStatus.DISCONNECTED)
        log.info("GPU stopped")
        return "ok"
    except Exception as exc:
        log.error("GPU stop failed: %s", exc)
        hooks.gpu_warning(i18n.tr("msg.gpu_stop_failed") + "\n" + provider.manual_instructions(i18n.get_language()))
        return "failed"
