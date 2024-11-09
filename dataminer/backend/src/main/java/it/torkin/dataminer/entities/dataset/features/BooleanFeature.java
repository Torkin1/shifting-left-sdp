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
public class BooleanFeature extends Feature<Boolean> {

    private Boolean value;
    
    public BooleanFeature(String name, Boolean value) {
        super(name);
        this.value = value;
    }
    
}
