package com.donald.demo.temporaldemoserver.hello.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@Setter
@Getter
@NoArgsConstructor
public class Person {
    private String firstName;
    private String lastName; 
    public String toString(){
        return firstName + " " + lastName;
    }
    public Person(String firstName, String lastName)
    {
        this.firstName=firstName;
        this.lastName=lastName;
    }
}
