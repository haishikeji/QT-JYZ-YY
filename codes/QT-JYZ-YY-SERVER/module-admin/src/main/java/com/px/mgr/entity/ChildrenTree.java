package com.px.mgr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ChildrenTree  implements Serializable {
    //ID
    private  Integer id;
    //标题
    private  String  title;
    //是否展开
    private  Boolean  spread;
}
