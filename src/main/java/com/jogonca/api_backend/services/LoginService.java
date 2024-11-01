package com.jogonca.api_backend.services;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.jogonca.api_backend.exceptions.NotFoundException;
import com.jogonca.api_backend.exceptions.OperationErrorException;
import com.jogonca.api_backend.mapper.Mapper;
import com.jogonca.api_backend.models.Item;
import com.jogonca.api_backend.models.User;
import com.jogonca.api_backend.models.dtos.receiveDTOs.ItemDTO;
import com.jogonca.api_backend.models.dtos.receiveDTOs.RecoveryRequestDTO;
import com.jogonca.api_backend.models.dtos.receiveDTOs.UserDTO;
import com.jogonca.api_backend.models.dtos.sendDTOs.AddItemUser;
import com.jogonca.api_backend.models.dtos.sendDTOs.UserSendDTO;
import com.jogonca.api_backend.repositories.ItemRepository;
import com.jogonca.api_backend.repositories.UserRepository;
import com.jogonca.api_backend.utils.Utils;

import jakarta.transaction.Transactional;

@Service
public class LoginService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    private static String phash;

    @Transactional
    public List<ItemDTO> addItem(AddItemUser json) {
        User user = userRepository.findByIdentifier(json.getIdentifier())
            .orElseThrow(() -> new NotFoundException("Não foi encontrado usuário com este e-mail: " + json.getIdentifier()));

        Item item = itemRepository.findByIdentifier(json.getNameItem())
            .orElseThrow(() -> new NotFoundException("Não foi encontrado Item com este nome: " + json.getNameItem()));

        addItem(user, item);
        List<Item> listItem = new ArrayList<>(user.getItens());
        List<ItemDTO> list = new ArrayList<>();
        for(Item i: listItem){
            ItemDTO dto = Mapper.parseObject(i, ItemDTO.class);
            dto.setKey(i.getId());
            list.add(dto);
        }
        return list;
    }

    @Transactional
    public List<ItemDTO> getItens(UserSendDTO json) {
        User user = userRepository.findByIdentifier(json.getIdentifier())
            .orElseThrow(() -> new NotFoundException("Não foi encontrado usuário com este e-mail: " + json.getIdentifier()));
        List<Item> listItem = new ArrayList<>(user.getItens());
        List<ItemDTO> list = new ArrayList<>();
        for(Item i: listItem){
            ItemDTO dto = Mapper.parseObject(i, ItemDTO.class);
            dto.setKey(i.getId());
            list.add(dto);
        }
        return list;
    }

    public String recoveryPassEmail(RecoveryRequestDTO json) {
        if (!Utils.isValidEmail(json.getIdentifier())) {
            throw new OperationErrorException("Email não válido!");
        }

        UserDTO user = userService.findByEmail(json.getIdentifier());
        if (user == null) {
            throw new NotFoundException("Não foi encontrado usuário com este e-mail: " + json.getIdentifier());
        }

        String token = Utils.generateRecoveryToken();
        user.setRecoveryToken(token);
        user.setTokenExpiration(Instant.now().plus(15, ChronoUnit.MINUTES).atZone(ZoneId.systemDefault()));
        userService.update(Mapper.parseObject(user, UserSendDTO.class));

        phash = String.valueOf(json.getPass().hashCode());

        emailService.sendPasswordRecoveryEmail(user.getEmail(), "http://localhost:8080/user/confirmRecovery/" + token);

        return token;
    }

    @Transactional
    public UserDTO confirmRecovery(@PathVariable String token) {
        UserDTO user = userService.findByToken(token);

        if (user == null) {
            throw new NotFoundException("Usuário não encontrado");
        }

        // Verificar token e expiração
        if (!user.getRecoveryToken().equals(token) || user.getTokenExpiration().isBefore(Instant.now().atZone(ZoneId.systemDefault()))) {
            throw new OperationErrorException("Token inválido ou expirado.");
        }

        // Atualizar senha e limpar o token
        user.setPasswordHash(phash);
        phash = null;
        user.setRecoveryToken(null);
        user.setTokenExpiration(null);
        userService.update(Mapper.parseObject(user, UserSendDTO.class));

        return user;
    }

    // Adiciona o usuário ao item
    public void addItem(User user, Item item) {
        user.getItens().add(item);
        item.getUsers().add(user);
    }

    // Remove o usuário do item
    public void removeItem(User user, Item item) {
        user.getItens().remove(item);
        item.getUsers().remove(user);
    }
}
