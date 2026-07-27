import AsyncStorage from '@react-native-async-storage/async-storage';
import React from 'react';
import { Linking } from 'react-native';
import type { WidgetTaskHandlerProps } from 'react-native-android-widget';
import { MyWidget } from './widget';

const nameToWidget = {
  // Hello will be the **name** with which we will reference our widget.
  widget: MyWidget,
};

async function getStorageData (){
    const jsonData = await AsyncStorage.getItem("widget");
    if(jsonData){
      return JSON.parse(jsonData)
    }}

export async function widgetTaskHandler(props: WidgetTaskHandlerProps) {
  const widgetInfo = props.widgetInfo;
  const Widget = nameToWidget[widgetInfo.widgetName as keyof typeof nameToWidget];

  const date = new Date();
      const formattedDate = date.toLocaleDateString('en-GB', {
        day: 'numeric',
        month: 'short'
      });

  switch (props.widgetAction) {
    case 'WIDGET_ADDED':{
      const dailyTasks = await getStorageData();
      props.renderWidget(<Widget task={dailyTasks} date={formattedDate} />);
      break;
    }
    case 'WIDGET_UPDATE':{
      const dailyTasks = await getStorageData();
      props.renderWidget(<Widget task={dailyTasks} date={formattedDate} />);
      break;
    }
    case 'WIDGET_RESIZED':{
      const dailyTasks = await getStorageData();
      props.renderWidget(<Widget task={dailyTasks} date={formattedDate} />);
      break;
    }
    case 'WIDGET_DELETED':
      // Not needed for now
      break;
    case 'WIDGET_CLICK':{
      if(props.clickAction === "OPEN_APP"){
        Linking.openURL("remainderr://index");}
      break;
    }
    default:
      break;
  }
}