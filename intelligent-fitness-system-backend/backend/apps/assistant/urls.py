from django.urls import path
from .views import (
    assistant_chat,
    assistant_chat_stream,
    chat_messages,
    clear_chat_messages,
    weekly_plan,
    regenerate_weekly_plan,
)

urlpatterns = [
    # Chat
    path("chat/", assistant_chat, name="assistant-chat"),
    path("chat/stream/", assistant_chat_stream, name="assistant-chat-stream"),
    path("chat/messages/", chat_messages, name="assistant-chat-messages"),
    path("chat/messages/clear/", clear_chat_messages, name="assistant-chat-messages-clear"),

    # Weekly plan
    path("weekly-plan/", weekly_plan, name="assistant-weekly-plan"),
    path("weekly-plan/regenerate/", regenerate_weekly_plan, name="assistant-weekly-plan-regenerate"),
]
