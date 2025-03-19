import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

export class WebSocketService {
    constructor() {
        this.stompClient = null;
        this.messageHandlers = new Map();
        this.summaryHandlers = new Map();
    }

    connect() {
        const socket = new SockJS('http://localhost:8080/ws');
        this.stompClient = Stomp.over(socket);

        this.stompClient.connect({}, () => {
            console.log('WebSocket Connected');
        }, error => {
            console.error('WebSocket Error:', error);
        });
    }

    subscribeToMessages(contactId, callback) {
        const subscription = this.stompClient.subscribe(
            `/topic/messages/${contactId}`,
            message => {
                const messageData = JSON.parse(message.body);
                callback(messageData);
            }
        );
        this.messageHandlers.set(contactId, subscription);
    }

    subscribeToSummary(contactId, callback) {
        const subscription = this.stompClient.subscribe(
            `/topic/summary/${contactId}`,
            message => {
                const summaryData = JSON.parse(message.body);
                callback(summaryData);
            }
        );
        this.summaryHandlers.set(contactId, subscription);
    }

    unsubscribe(contactId) {
        const messageSubscription = this.messageHandlers.get(contactId);
        if (messageSubscription) {
            messageSubscription.unsubscribe();
            this.messageHandlers.delete(contactId);
        }

        const summarySubscription = this.summaryHandlers.get(contactId);
        if (summarySubscription) {
            summarySubscription.unsubscribe();
            this.summaryHandlers.delete(contactId);
        }
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect();
        }
    }
} 