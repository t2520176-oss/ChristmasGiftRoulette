from __future__ import annotations

from ..settings import Settings
from .base import GenerationProvider
from .http_generic import GenericHttpProvider
from .mock import MockProvider
from .runpod import RunPodProvider

PROVIDER_CLASSES = {"mock": MockProvider, "generic_http": GenericHttpProvider, "runpod": RunPodProvider}


def create_provider(settings: Settings) -> GenerationProvider:
    cls = PROVIDER_CLASSES.get(settings.provider, MockProvider)
    return cls(settings)
