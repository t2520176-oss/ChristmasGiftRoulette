# FigureCraft cloud worker image (CUDA).
#
# STATUS: the server part (FastAPI + protocol) is tested with ENGINE=mock. The TRELLIS / Hunyuan3D install
# sections below follow the upstream READMEs from memory and were NOT built or run on a GPU while this repo
# was written - expect to adjust versions (CUDA / torch / extension builds) when you build it the first time.
#
# Build:  docker build -t figurecraft-worker --build-arg ENGINE=trellis .
# Run  :  docker run --gpus all -p 8000:8000 --env-file .env figurecraft-worker
# RunPod serverless: set the container start command to `python -u runpod_handler.py`.
FROM pytorch/pytorch:2.4.0-cuda12.1-cudnn9-devel

ARG ENGINE=trellis
ENV DEBIAN_FRONTEND=noninteractive PYTHONUNBUFFERED=1 ENGINE=${ENGINE} HF_HOME=/models/hf

RUN apt-get update && apt-get install -y --no-install-recommends git build-essential ninja-build libgl1 libglib2.0-0 \
    libgomp1 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ---- engine-specific installation --------------------------------------------------------------------------
# TRELLIS (MIT). Upstream: https://github.com/microsoft/TRELLIS  (see its setup.sh for the exact flags)
RUN if [ "$ENGINE" = "trellis" ]; then \
      git clone --recurse-submodules https://github.com/microsoft/TRELLIS.git /opt/TRELLIS && \
      cd /opt/TRELLIS && . ./setup.sh --basic --xformers --flash-attn --diffoctreerast --spconv --mipgaussian --kaolin --nvdiffrast ; \
    fi
ENV PYTHONPATH=/opt/TRELLIS

# Hunyuan3D-2 (check the license before use!). Upstream: https://github.com/Tencent/Hunyuan3D-2
RUN if [ "$ENGINE" = "hunyuan3d" ]; then \
      git clone https://github.com/Tencent/Hunyuan3D-2.git /opt/Hunyuan3D-2 && cd /opt/Hunyuan3D-2 && \
      pip install --no-cache-dir -r requirements.txt && pip install --no-cache-dir -e . && \
      cd hy3dgen/texgen/custom_rasterizer && python setup.py install && \
      cd ../differentiable_renderer && python setup.py install ; \
    fi
# ------------------------------------------------------------------------------------------------------------

COPY config.py worker.py runpod_handler.py ./
COPY engines ./engines
EXPOSE 8000
CMD ["python", "-u", "worker.py"]
