package one.bio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @Auther: 18517
 * @Date: 2022/5/30 16:18
 * @Description:
 */
@Data
@AllArgsConstructor
public class Message implements Serializable {
    private String content;
}
