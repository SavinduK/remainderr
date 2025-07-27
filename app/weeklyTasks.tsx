import AsyncStorage from '@react-native-async-storage/async-storage';
import { useRouter } from 'expo-router';
import React, { useEffect, useState } from "react";
import { Button, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from 'react-native-safe-area-context';

export default function Index() {

  const [inputText,setInputText] = useState('');//saves and updates the text in textbox
  
  const [data,setData] = useState<any[]>([]);
  const KEY = 'dbKey';

  const router = useRouter()

  //refresh list
  useEffect(() => {
      reloadItems();
      }, []);
 
  //save to async storage
  const storeData = async (value: any) => {
  try {
    const jsonValue = await AsyncStorage.getItem(KEY);
    if(jsonValue != null){
      let data = JSON.parse(jsonValue);
      //console.log(data);
      data.push([value,'2']);
      setData(data);
      try {
        await AsyncStorage.setItem(KEY, JSON.stringify(data));
      } catch (e) {
           console.error('Failed to save data', e);
         }
    }else{
      let data = [];
      data.push([value,'2'])
      setData(data)
      try {
        await AsyncStorage.setItem(KEY, JSON.stringify(data));
      } catch (e) {
           console.error('Failed to save data', e);
         }
    }
  } catch (e) {
    console.error('Failed to fetch data', e);
    alert('error')
  }
};
  //handle data input
  const handleInput = () =>{
      setInputText('')
      if (inputText != ''){
        storeData(inputText)
      }else{
        reloadItems()
      }
      
  }
  //clear data from storage
  const clearAsync = async () =>{
    try{
        await AsyncStorage.clear()
        setData([])
        try {
          await AsyncStorage.setItem(KEY, JSON.stringify([]));
       } catch (e) {
           console.error('Failed to save data', e);
           alert('error')
      }
    }catch(e){
      console.log(e)
    }
  }

  //handle delete(for some bloody reason, async storage does not seem to update from pre defined function.had to call it again)
  const handleDelete = async(index:number)=>{
      const newArray = data.filter((_,i)=> i !== index)
      setData(newArray)
      try {
          await AsyncStorage.setItem(KEY, JSON.stringify(newArray));
       } catch (e) {
           console.error('Failed to save data', e);
      }}
  //reload items
  const reloadItems = async() =>{
    try {
    const jsonValue = await AsyncStorage.getItem(KEY);
    if(jsonValue != null){
      let data = JSON.parse(jsonValue);
      console.log(data)
      setData(data)
    }else{
      let data: any[] = [];
      setData(data)
    }
  } catch (e) {
    console.error('Failed to fetch data', e);
    alert('error')
  }}

  //view components
  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Weekly Tasks</Text>

      <View style={styles.inputRow}>
        <TextInput placeholder='Add something to remember...' value={inputText} onChangeText={setInputText} style={styles.input}/>
        <Button title="Add" onPress={handleInput} color='#43B581'/>
      </View>

      <View style={styles.inputRow}>
        <Button title='Daily' onPress={() => router.push('/')} color='#4A90E2'/>
        <Button title='Weekly' onPress={() => router.push('/weeklyTasks')} color='#4A90E2'/>
        <Button title='Projects' onPress={() => router.push('/projects')} color='#4A90E2'/>
      </View>
      
      <ScrollView style={styles.scroll} contentContainerStyle={{width:'100%'}}>
        {data.map((item,index)=>(
            item[1] == '2' ? (
            <View key={index} style={styles.itemRow}>
                  <Text style={styles.itemText}>{item[0]}</Text>
                  <Button title='delete' onPress={()=>handleDelete(index)} color='#FF6B6B'/>
            </View>):null
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({    
       container: {
        flex: 1,
        padding: 20,
        backgroundColor: '#f0f0f0',
        alignItems:'center',
        fontFamily:'System'
      },
      title: {
        fontSize: 20,
        fontWeight: 'bold',
        marginBottom: 20,
        textAlign: 'center',
        fontFamily:'System'
      },
      inputRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 20,
        gap: 10,
        justifyContent:'space-between',
        fontFamily:'System',
      },
      input: {
        flex: 1,
        borderWidth: 1,
        borderColor: '#ccc',
        borderRadius: 5,
        padding: 10,
        backgroundColor: '#fff',
        fontFamily:'System',
      },
      scroll: {
        flex: 1,
      },
      itemRow: {
        flexDirection: 'row',
        alignItems: 'center',
        width:'100%',
        padding: 10,
        marginVertical: 5,
        backgroundColor: '#fff',
        borderRadius: 5,
        shadowColor: '#000',
        shadowOpacity: 0.1,
        shadowRadius: 5,
        elevation: 2,
        fontFamily:'System',
        
      },
      itemText: {
        flex: 1,
        fontSize: 16,
        fontFamily:'System',
      },
})