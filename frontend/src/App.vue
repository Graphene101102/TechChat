<template>
  <div class="app">
    <div class="contacts">
      <contact-list 
        :contacts="contacts"
        :selected-contact="selectedContact"
        @select-contact="handleSelectContact"
      />
    </div>
    <div class="chat">
      <chat-window 
        v-if="selectedContact"
        :contact="selectedContact"
        :messages="messages"
      />
      <div v-else class="no-chat">
        Chọn một cuộc trò chuyện để bắt đầu
      </div>
    </div>
  </div>
</template>

<script>
import ContactList from './components/ContactList.vue'
import ChatWindow from './components/ChatWindow.vue'
import axios from 'axios'

export default {
  name: 'App',
  components: {
    ContactList,
    ChatWindow
  },
  data() {
    return {
      contacts: [],
      selectedContact: null,
      messages: [],
      loading: false,
      error: null
    }
  },
  methods: {
    async loadContacts() {
      try {
        this.loading = true;
        const response = await axios.get('/api/chat/contacts');
        if (response.data.success) {
          this.contacts = response.data.data;
        } else {
          console.error('Error:', response.data.message);
        }
      } catch (error) {
        console.error('Error loading contacts:', error);
        this.error = 'Không thể tải danh sách liên hệ';
      } finally {
        this.loading = false;
      }
    },
    async handleSelectContact(contact) {
      try {
        this.loading = true;
        this.selectedContact = contact;
        const response = await axios.get(`/api/chat/messages/${contact.id}`);
        if (response.data.success) {
          this.messages = response.data.data;
        } else {
          console.error('Error:', response.data.message);
        }
      } catch (error) {
        console.error('Error loading messages:', error);
        this.error = 'Không thể tải tin nhắn';
      } finally {
        this.loading = false;
      }
    }
  },
  async mounted() {
    await this.loadContacts();
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: Arial, sans-serif;
}

.app {
  display: flex;
  height: 100vh;
  background-color: #fff;
}

.contacts {
  width: 300px;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.no-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  font-size: 1.2em;
  background-color: #f9f9f9;
}

.loading {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 10px 20px;
  border-radius: 5px;
}

.error {
  color: red;
  padding: 10px;
  text-align: center;
  background-color: #fee;
}
</style>
