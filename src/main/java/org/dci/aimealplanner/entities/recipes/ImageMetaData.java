package org.dci.aimealplanner.entities.recipes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ImageMetaData {
    @Id
    @NotBlank(message = "Public ID cannot be blank")
    private String publicId;

    @NotBlank(message = "Image URL cannot be blank")
    @Column(nullable = false)
    private String imageUrl;
}
