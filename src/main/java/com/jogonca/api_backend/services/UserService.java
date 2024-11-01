package com.jogonca.api_backend.services;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jogonca.api_backend.exceptions.NotFoundException;
import com.jogonca.api_backend.interfaces.AbstractCrudService;
import com.jogonca.api_backend.mapper.Mapper;
import com.jogonca.api_backend.models.User;
import com.jogonca.api_backend.models.dtos.receiveDTOs.UserDTO;
import com.jogonca.api_backend.models.dtos.sendDTOs.UserSendDTO;
import com.jogonca.api_backend.repositories.UserRepository;

import jakarta.persistence.Tuple;

@Service
public class UserService extends AbstractCrudService<UserDTO, User, UserSendDTO, String>{

    @Autowired
    private UserRepository userRepository;

    public UserService(@Autowired UserRepository repository) {
        super(User.class.getName(), repository, User.class, UserDTO.class);
    }

    protected void updateData(User entity, UserDTO i) {
        if(i.getUsername() != null){
            entity.setUsername(i.getUsername());
        }
        if(i.getEmail() != null){
            entity.setEmail(i.getEmail());
        }
        if(i.getPasswordHash() != null){
            entity.setPasswordHash(i.getPasswordHash());
        }
        if(i.getCoins() == null){
            entity.setCoins(0L);
        }else{
            entity.setCoins(i.getCoins());
        }
        
        entity.setRecoveryToken(i.getRecoveryToken());
        entity.setTokenExpiration(i.getTokenExpiration());
    }
    
    public UserDTO findByEmail(String email){
        Optional<User> user = repository.findByIdentifier(email);
        if(user.isEmpty()){
            throw new NotFoundException("E-mail não encontrado ou usuario não utilizando e-mail informado!");
        }
        return Mapper.parseObject(user.get(), UserDTO.class);
    }

    public User findByEmailEntity(String email){
        Optional<User> user = repository.findByIdentifier(email);
        if(user.isEmpty()){
            throw new NotFoundException("E-mail não encontrado ou usuario não utilizando e-mail informado!");
        }
        return user.get();
    }

    public UserDTO findByToken(String token){
        Tuple tuple = userRepository.findByToken(token);
        return new UserDTO(
            tuple.get("id", Long.class),
            tuple.get("username", String.class),
            tuple.get("email", String.class),
            tuple.get("coins", Long.class),
            tuple.get("password_hash", String.class),
            (tuple.get("creat_at") == null) ? null :
            tuple.get("creat_at", OffsetDateTime.class).toZonedDateTime(),
            token,
            (tuple.get("token_expiration") == null) ? null :
            tuple.get("token_expiration", OffsetDateTime.class).toZonedDateTime()
        );
    }

}
