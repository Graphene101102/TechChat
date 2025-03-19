<template>
  <div class="chat-window">
    <div class="chat-header">
      <h3>{{ contact.name }}</h3>
    </div>
    <div class="messages" ref="messageContainer">
      <div
        v-for="message in sortedMessages"
        :key="message.id"
        class="message"
        :class="{ 'message-own': message.sender === 'BOT' }"
      >
        <div class="message-content">{{ message.content }}</div>
        <div class="message-time">
          {{ formatTime(message.timestamp) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChatWindow',
  props: {
    contact: {
      type: Object,
      required: true
    },
    messages: {
      type: Array,
      required: true,
      default: () => []
    }
  },
  computed: {
    sortedMessages() {
      return [...this.messages].sort((a, b) => {
        return new Date(a.timestamp) - new Date(b.timestamp)
      })
    }
  },
  methods: {
    formatTime(timestamp) {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      return date.toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
    },
    scrollToBottom() {
      if (this.$refs.messageContainer) {
        this.$refs.messageContainer.scrollTop = this.$refs.messageContainer.scrollHeight
      }
    }
  },
  watch: {
    messages: {
      handler() {
        this.$nextTick(this.scrollToBottom)
      },
      deep: true
    }
  },
  mounted() {
    this.scrollToBottom()
  }
}
</script>

<style scoped>
.chat-window {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #fff;
}

.chat-header h3 {
  margin: 0;
  font-size: 1.2em;
  color: #333;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message {
  max-width: 70%;
  padding: 10px 15px;
  border-radius: 15px;
  margin-bottom: 10px;
}

.message-content {
  word-wrap: break-word;
}

.message-time {
  font-size: 0.8em;
  color: #666;
  margin-top: 5px;
}

.message {
  background-color: #f0f0f0;
  align-self: flex-start;
}

.message-own {
  background-color: #e3f2fd;
  align-self: flex-end;
}
</style>
