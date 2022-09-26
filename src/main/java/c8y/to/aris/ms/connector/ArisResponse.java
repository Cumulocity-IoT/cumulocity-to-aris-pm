package c8y.to.aris.ms.connector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ArisResponse<T> {
    @With private String message;
    @With private boolean ok;
    @With private T result;
}
