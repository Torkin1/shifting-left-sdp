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
public class StringFeature extends Feature<String> {

    private String value;
    
    public StringFeature(String name, String value) {
        super(name);
        this.value = value;
    }
    
}
