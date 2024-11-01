package com.jogonca.api_backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    
    public NotFoundException(){
        super("Não foi encontrado a entidade, verifique se inseriu os dados da maneira correta");
    }

    public NotFoundException(String msg){
        super(msg);
    }
    
}