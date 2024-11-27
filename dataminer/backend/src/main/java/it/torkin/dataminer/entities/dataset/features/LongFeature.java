package it.torkin.dataminer.entities.dataset.features;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Entity
@ToString(callSuper = true)
public class LongFeature extends Feature<Long> {

    private Long value;
    
    public LongFeature(String name, Long value) {
        super(name);
        this.value = value;
    }
    
}
