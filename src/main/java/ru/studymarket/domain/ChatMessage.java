package ru.studymarket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserAccount sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 1200)
    private String body;

    @Column(nullable = false)
    private boolean readByRecipient = false;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    protected ChatMessage() {
    }

    public ChatMessage(UserAccount sender, UserAccount recipient, Product product, String body) {
        this.sender = sender;
        this.recipient = recipient;
        this.product = product;
        this.body = body;
    }

    @PrePersist
    void prePersist() {
        sentAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getSender() {
        return sender;
    }

    public UserAccount getRecipient() {
        return recipient;
    }

    public Product getProduct() {
        return product;
    }

    public String getBody() {
        return body;
    }

    public boolean isReadByRecipient() {
        return readByRecipient;
    }

    public void setReadByRecipient(boolean readByRecipient) {
        this.readByRecipient = readByRecipient;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
