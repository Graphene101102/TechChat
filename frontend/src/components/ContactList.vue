<template>
  <div class="contact-list">
    <div class="contact-list-header">
      <h2>Danh sách chat</h2>
    </div>
    <div class="contact-items">
      <div
        v-for="contact in contacts"
        :key="contact?.id || index"
        class="contact-item"
        :class="{ 'active': isSelected(contact) }"
        @click="selectContact(contact)"
      >
        <div class="contact-info">
          <div class="contact-name">{{ contact?.name || 'Không tên' }}</div>
          <div class="last-message">{{ contact?.lastMessage || 'Chưa có tin nhắn' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ContactList',
  props: {
    contacts: {
      type: Array,
      default: () => []
    },
    selectedContact: {
      type: Object,
      default: null
    }
  },
  methods: {
    isSelected(contact) {
      if (!contact || !this.selectedContact) return false;
      return contact.id === this.selectedContact.id;
    },
    selectContact(contact) {
      if (!contact) return;
      this.$emit('select-contact', contact);
    }
  }
}
</script>

<style scoped>
.contact-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.contact-list-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  background-color: #fff;
}

.contact-list-header h2 {
  margin: 0;
  font-size: 1.2em;
  color: #333;
}

.contact-items {
  flex: 1;
  overflow-y: auto;
}

.contact-item {
  padding: 15px 20px;
  cursor: pointer;
  border-bottom: 1px solid #e0e0e0;
  background-color: #fff;
  transition: all 0.3s ease;
}

.contact-item:hover {
  background-color: #f0f0f0;
}

.contact-item.active {
  background-color: #e3f2fd;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.contact-name {
  font-weight: 500;
  color: #333;
}

.last-message {
  font-size: 0.9em;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
