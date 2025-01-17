package com.px.mgr.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * fullCalendar相应数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FullCalendarJson implements Serializable {
     Integer  resourceId;
     Integer id;
     Integer status;
     String  title;
     String color;
     Date start;
     Date   end;
}
