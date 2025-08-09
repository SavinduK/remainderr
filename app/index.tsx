import { FontAwesome5, Ionicons } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system';
import * as Print from "expo-print";
import { useRouter } from 'expo-router';
import * as Sharing from "expo-sharing";
import React, { useEffect, useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from 'react-native-safe-area-context';

export default function Index() {

  const [fileName,setFileName] = useState('');
  const [isSharing, setIsSharing] = useState(false);
  const router = useRouter();
  
  const [fileList, setFileList] = useState<string[]>([]);

  const loadFiles = async () => {
    try {
      const files = await FileSystem.readDirectoryAsync(FileSystem.documentDirectory!);
      const txtFiles = files.filter(file => file.toLowerCase().endsWith('.txt'));
      const fileUris = txtFiles.map(file => FileSystem.documentDirectory + file);
      setFileList(fileUris);
    } catch (error) {
      console.error('Error loading files:', error);
    }
  };

  useEffect(() => {
    loadFiles();
  }, []);

  const openFile =  (uri:any) => {
    let path = uri.split('/').pop()
    let filename = path.slice(0,path.lastIndexOf('.'))
    router.push(`/${filename}`)
  };

  const deleteFile = async (uri: string) => {
    try {
      await FileSystem.deleteAsync(uri);
      await loadFiles(); // Refresh list
    } catch (error) {
     console.log('Error', 'Could not delete file.');
    }
  };

  const createFile = async() =>{
    const fileUri = FileSystem.documentDirectory + fileName+'.txt';
    const fileContents = '';
    console.log('presses')
    if(fileName=='')return
    try {
        await FileSystem.writeAsStringAsync(fileUri, fileContents, {
        encoding: FileSystem.EncodingType.UTF8,
        });
        console.log('File saved to:', fileUri);
        setFileName('');
        router.push(`/${fileName}`);
        
    } catch (error) {
        console.error('Error saving file:', error);
    }}
  
  const shareFile = async(uri:string) =>{
      if (isSharing) return; // Prevent multiple presses
          setIsSharing(true);
          const fileUri = uri  
          try {
                  const fileExists = await FileSystem.getInfoAsync(fileUri);
                  if (fileExists.exists) {
                    const text = await FileSystem.readAsStringAsync(fileUri);
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
                                
                  } else {
                    console.log('[File not found]');
                  }
                } catch (error) {
                  console.log('[Error reading file]');
                  //console.error('Error reading file:', error);
                }
  }


  //view components
  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>My Notes</Text>

      <View style={styles.inputRow}>
        <TextInput placeholder='Enter File Name...' value={fileName} onChangeText={setFileName} style={styles.input}/>
        <Pressable style={styles.button} onPress={createFile}>
           <FontAwesome5 name='file-medical' size={25} color="#555" />
        </Pressable>
      </View>

      <ScrollView style={styles.scroll}>
      <Text style={styles.title}>Saved Files</Text>
      {fileList.length === 0 ? (
        <Text style={styles.text}>No files found.</Text>
      ) : (
        fileList.map((uri, index) => (
          <View key={index} style={styles.fileItem}>
            <Text style={styles.text}>{uri.split('/').pop()}</Text>
            <View style={styles.buttonRow}>
              <Pressable style={styles.button} onPress={() => openFile(uri)}>
                  <FontAwesome5 name='marker' size={20} color="#555" />
                </Pressable>
              <Pressable style={styles.button} onPress={() => deleteFile(uri)}>
                  <Ionicons name='trash-outline' size={20} color="#555" />
                </Pressable>
              <Pressable style={styles.button} onPress={() => shareFile(uri)}>
                  <Ionicons name='share-social' size={20} color="#555" />
                </Pressable>
            </View>
          </View>
        ))
      )}
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
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 20,
        textAlign: 'center',
        color:'#555',
        fontFamily:'System',
      },
      inputRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 20,
        gap: 10,
        width:'100%',
        padding:10,
        borderRadius:8,
        justifyContent:'space-between',
        backgroundColor:'#fff',
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
      fileItem: {
        flexDirection:'row',
        justifyContent:'space-between',
        marginBottom: 10,
        width:'100%',
        backgroundColor: '#fff',
        padding: 10,
        borderRadius: 8,
      },
      buttonRow :{
        flexDirection:'row',
        justifyContent:'space-between',
        gap:10,
      },
      button:{
        borderWidth:1,
        borderColor:'#ccc',
        padding:5,
        borderRadius:5,
      },
      text: {
        fontSize: 16,
        marginBottom: 5,
        color:'#555'
      },
    
})