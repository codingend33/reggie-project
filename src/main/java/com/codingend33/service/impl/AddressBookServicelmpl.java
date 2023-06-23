package com.codingend33.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codingend33.entity.AddressBook;
import com.codingend33.mapper.AddressBookMapper;
import com.codingend33.service.AddressBookService;
import org.springframework.stereotype.Service;

@Service
public class AddressBookServicelmpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService{
}
