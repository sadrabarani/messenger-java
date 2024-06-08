package User;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Block {
    private String blocker;
    private String blocked;
    public Block(String blocker, String blocked)
    {
        this.blocker = blocker;
        this.blocked = blocked;
    }
}
