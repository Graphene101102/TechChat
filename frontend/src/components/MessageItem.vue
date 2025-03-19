<template>
  <div class="message-item" :class="messageClass">
    <div class="message-content">
      <div class="message-text">{{ message.content }}</div>
      <div class="message-time">{{ formatTime }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MessageItem',
  props: {
    message: {
      type: Object,
      required: true,
      validator: (message) => {
        return message.content && message.sender && message.timestamp;
      }
    }
  },
  computed: {
    messageClass() {
      return {
        'message-user': this.message.sender === 'USER',
        'message-bot': this.message.sender === 'BOT'
      }
    },
    formatTime() {
      return new Date(this.message.timestamp).toLocaleTimeString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.message-item {
  display: flex;
  margin: 8px 0;
  max-width: 70%;
}

.message-user {
  margin-left: auto;
}

.message-bot {
  margin-right: auto;
}

.message-content {
  padding: 8px 12px;
  border-radius: 16px;
  position: relative;
}

.message-user .message-content {
  background-color: #0084ff;
  color: white;
}

.message-bot .message-content {
  background-color: #f1f0f0;
  color: black;
}

.message-text {
  word-wrap: break-word;
  margin-bottom: 4px;
}

.message-time {
  font-size: 0.75rem;
  color: #8e8e8e;
  text-align: right;
}

.message-bot .message-time {
  color: #65676B;
}
</style>
