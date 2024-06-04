import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
public class Message {
    private String content;
    private LocalDateTime dateTime;
    private User sender;
    private User reciever;

    public Message(String content, LocalDateTime dateTime, User sender, User reciever) {
        this.content = content;
        this.dateTime = dateTime;
        this.sender = sender;
        this.reciever = reciever;
    }

    @Override
    public String toString()
    {
        return this.content + " - " + this.dateTime.toString() + " - by " + this.sender.getUserName() + "\n";
    }
}
