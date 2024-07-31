package it.torkin.dataminer.entities.jira;

import com.google.gson.annotations.SerializedName;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class AvatarUrls{
    @SerializedName("16x16") 
    private String _16x16;
    @SerializedName("24x24") 
    private String _24x24;
    @SerializedName("32x32") 
    private String _32x32;
    @SerializedName("48x48") 
    private String _48x48;
}
