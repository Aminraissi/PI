package org.exemple.gestionformation.service;

import org.exemple.gestionformation.entity.LeconVideo;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


public interface LeconVideoService {
    LeconVideo create(Long moduleId, LeconVideo leconVideo);
    List<LeconVideo> getAll();
    LeconVideo getById(Long id);
    List<LeconVideo> getByModule(Long moduleId);
    LeconVideo update(Long id, LeconVideo newData);
    void delete(Long id);
    String uploadVideo(MultipartFile file);
}
