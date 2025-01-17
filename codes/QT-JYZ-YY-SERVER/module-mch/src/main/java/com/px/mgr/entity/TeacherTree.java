package com.px.mgr.entity;


import com.px.db.model.bean.AdminUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TeacherTree implements Serializable {
     //ID
     private  Integer id;
     //标题
     private  String  title;
     //是否展开
     private  Boolean  spread;

     private   List<ChildrenTree>  children;
}
