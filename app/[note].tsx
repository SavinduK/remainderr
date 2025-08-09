import { FontAwesome5 } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system';
import * as Print from "expo-print";
import { useLocalSearchParams, useRouter } from "expo-router";
import * as Sharing from "expo-sharing";
import React, { useEffect, useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

export default function Index() {

    const router = useRouter();
    const [text,setText] = useState('');
    const {note} = useLocalSearchParams();

    const [isSharing, setIsSharing] = useState(false);

    const createFile = ()=>{
         setText('')
         router.push('/')
    }
    const openFile = () =>{
        router.push('/')
    }
    const saveFile = async() =>{
        const fileUri = FileSystem.documentDirectory + `${note}.txt`;
        const fileContents = text;

      try {
          await FileSystem.writeAsStringAsync(fileUri, fileContents, {
          encoding: FileSystem.EncodingType.UTF8,
        });
          console.log('File saved to:', fileUri);
        } catch (error) {
          console.error('Error saving file:', error);
         }}

  const loadContent = async() =>{
         try {
        const fileUri = FileSystem.documentDirectory + `${note}.txt`
        const fileExists = await FileSystem.getInfoAsync(fileUri);
        if (fileExists.exists) {
          const text = await FileSystem.readAsStringAsync(fileUri);
          setText(text);
        } else {
          console.log('[File not found]');
        }
      } catch (error) {
        console.log('[Error reading file]');
        //console.error('Error reading file:', error);
      }
    };

    const shareFile = async () => {
      if (isSharing) return; // Prevent multiple presses
      setIsSharing(true);
      const fileUri = FileSystem.documentDirectory + `${note}.txt`;
      const fileContents = text;

      try {
          await FileSystem.writeAsStringAsync(fileUri, fileContents, {
          encoding: FileSystem.EncodingType.UTF8,
        });
          console.log('File saved to:', fileUri);
        } catch (error) {
          console.error('Error saving file:', error);
        }

      try {
      const { uri } = await Print.printToFileAsync({
        html: `<pre style="font-size: 25px; font-family: monospace; font-weight: bold">${text}</pre>`,
      });

        // Check if sharing is available
        const available = await Sharing.isAvailableAsync();
        if (!available) {
          console.log("Sharing is not available on this device");
          return;
        }

        // Open share dialog
        await Sharing.shareAsync(uri,{mimeType: "application/pdf",dialogTitle: "Share My File"});
      } catch (error) {
        console.error("Error sharing file:", error);
      } finally {
      setIsSharing(false); //  Allow sharing again
      }
  };
    //on load refresh
    useEffect(()=>{
      loadContent()
    },[]);
    
  return (
   <SafeAreaView style={styles.container}>
    <View style={styles.headingRow}>
      <FontAwesome5 name='file-code' size={20} color='#555' />
      <Text style={styles.title}>{note}.txt - NotePad</Text>
    </View>
      
    <View style={styles.buttonRow}>
        <Pressable style={styles.button} onPress={createFile}>
          <Text style={styles.text}>New</Text>
        </Pressable>
        <Pressable style={styles.button} onPress={openFile}>
          <Text style={styles.text}>Open</Text>
        </Pressable>
        <Pressable style={styles.button} onPress={saveFile}>
          <Text style={styles.text}>Save</Text>
        </Pressable>
         <Pressable style={styles.button} onPress={shareFile}>
          <Text style={styles.text}>Share</Text>
        </Pressable>
    </View>
    <TextInput style={styles.input} multiline value={text} onChangeText={setText} placeholder="" />

   </SafeAreaView>
  );
}

const styles = StyleSheet.create({
    container:{
        flex: 1,
        padding:5,
        backgroundColor: '#f0f0f0',
        fontFamily:'System'
    },
    input:{
        flex:1,
        backgroundColor:'#ffffff',
        width:'100%',
        textAlignVertical: "top" , 
        borderRadius: 8,
        borderWidth:1,
        borderColor:'#ccc',
        color:'#555',
        fontSize:15,
        fontFamily:'System',  
    },
    title: {
        fontSize: 16,
        fontWeight: 'bold',
        color:'#555',
        fontFamily:'System',
      },
    buttonRow :{
        flexDirection:'row',
        justifyContent:'space-around',
        gap:5,
    },
    headingRow :{
        flexDirection:'row',
        justifyContent:'flex-start',
        padding:10,
        gap:10,
    },
    button:{
        padding:5,
    },
    text: {
        fontSize: 16,
        marginBottom: 5,
        fontWeight:'bold',
        color:'#555'
    },
})
