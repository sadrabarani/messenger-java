package message;

import lombok.Getter;
import lombok.Setter;
import user.User;

import java.time.LocalDateTime;


@Getter
@Setter
public class Message {
    private String content;
    private LocalDateTime dateTime;
    private User sender;
    private User reciever;
    private int type; // either 0 = chatroom message or 1 = pv message
    public Message(String content, LocalDateTime dateTime, User sender, User reciever, int type) {
        this.content = content;
        this.dateTime = dateTime;
        this.sender = sender;
        this.reciever = reciever;
        this.type = type;
    }

    @Override
    public String toString()
    {
        return this.content + " - " + this.dateTime.toString() + " - by " + this.sender.getUserName() + "\n";
    }
}
