package com.clou.ess;

import com.clou.ess.model.PackageModel;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Admin on 2018-03-20.
 */
public class StringTest {
    @Test
    public void  testString() {
        List<String> list = new ArrayList();
        list.add("packahe_data_1");
        PropertyDescriptor[] targetPds = BeanUtils.getPropertyDescriptors(PackageModel.class);
        for (PropertyDescriptor descriptor : targetPds) {
            list.add(descriptor.getName());
        }
        String str = StringUtils.join(list,",");
        System.out.println(str);
        String[] temp =str.split(",");
        System.out.println(temp);
    }
}
