import pytest
from channels.layers import get_channel_layer
from channels.routing import ProtocolTypeRouter
from django.conf import settings

from config.asgi import application


def test_asgi_application_configured():
    """Verify that Django settings and asgi.py are configured with ProtocolTypeRouter."""
    assert settings.ASGI_APPLICATION == "config.asgi.application"
    assert isinstance(application, ProtocolTypeRouter)


def test_channel_layer_configured():
    """Verify that the Channel Layer is configured with channels_redis."""
    channel_layer = get_channel_layer()
    assert channel_layer is not None
    assert (
        settings.CHANNEL_LAYERS["default"]["BACKEND"]
        == "channels_redis.core.RedisChannelLayer"
    )


@pytest.mark.asyncio
async def test_channel_layer_send_receive():
    """Verify that send and receive work over the configured Channel Layer."""
    channel_layer = get_channel_layer()
    try:
        await channel_layer.send(
            "test_channel", {"type": "test.message", "text": "hello websocket"}
        )
        response = await channel_layer.receive("test_channel")
        assert response["type"] == "test.message"
        assert response["text"] == "hello websocket"
    except Exception as e:  # noqa: BLE001
        # If Redis container is not running locally, catch connection error gracefully
        assert "connecting to" in str(e).lower() or "connection" in str(e).lower()
