import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import React, { useState } from 'react';
import {
  KeyboardAvoidingView, Modal, Platform, StyleSheet, Text, TextInput, TouchableOpacity, useColorScheme, View
} from 'react-native';
import { Colors } from './theme';

const STORAGE_KEY = 'user_passwords_list';

interface AddPasswordProps {
  visible: boolean;
  onClose: () => void;
  onSave: () => void; 
}

export default function AddPasswordModal({ visible, onClose, onSave }: AddPasswordProps) {
  const colorScheme = useColorScheme() ?? 'light';
  const theme = Colors[colorScheme];

  const [form, setForm] = useState({ site: '', user: '', pass: '' });

  const handleSave = async () => {
    if (!form.site || !form.user || !form.pass) return;

    const current = await AsyncStorage.getItem(STORAGE_KEY);
    const list = current ? JSON.parse(current) : [];
    const newList = [...list, { id: new Date().getTime(), site: form.site, username: form.user, password: form.pass }];
    
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(newList));
    
    setForm({ site: '', user: '', pass: '' });
    onSave(); 
    onClose();
  };

  return (
    <Modal visible={visible} animationType="slide" transparent={true} onRequestClose={onClose}>
      <View style={styles.overlay}>
        <KeyboardAvoidingView 
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'} 
          style={[styles.modalContent, { backgroundColor: theme.background, borderColor: theme.border }]}
        >
          <View style={styles.header}>
            <Text style={[styles.title, { color: theme.title }]}>New Entry</Text>
            <TouchableOpacity onPress={onClose}>
              <Ionicons name="close" size={24} color={theme.subtext} />
            </TouchableOpacity>
          </View>

          <View style={[styles.inputContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
            <TextInput 
              placeholder="Site / App" placeholderTextColor={theme.subtext} 
              style={[styles.input, { color: theme.text, borderBottomColor: theme.border }]} 
              value={form.site}
              onChangeText={(t) => setForm({...form, site: t})} 
            />
            <TextInput 
              placeholder="Username" placeholderTextColor={theme.subtext} 
              style={[styles.input, { color: theme.text, borderBottomColor: theme.border }]} 
              autoCapitalize="none"
              value={form.user}
              onChangeText={(t) => setForm({...form, user: t})} 
            />
            <TextInput 
              placeholder="Password" placeholderTextColor={theme.subtext} 
              secureTextEntry style={[styles.input, { color: theme.text, borderBottomWidth: 0 }]} 
              value={form.pass}
              onChangeText={(t) => setForm({...form, pass: t})} 
            />
          </View>

          <TouchableOpacity style={[styles.btn, { backgroundColor: theme.accent_light,borderColor:theme.accent }]} onPress={handleSave}>
            <Text style={[styles.btnText,{color:theme.accent}]}>Save</Text>
          </TouchableOpacity>
        </KeyboardAvoidingView>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {flex: 1,backgroundColor: 'rgba(0,0,0,0.6)',justifyContent: 'flex-end', },
  modalContent: {
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    padding: 24,
    paddingBottom: 40,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 25,
  },
  title: { fontSize: 22, fontWeight: '800' },
  inputContainer: { borderRadius: 15, borderWidth: 1, paddingHorizontal: 15 },
  input: { height: 50, borderBottomWidth: 1, fontSize: 16 },
  btn: { height: 50, borderRadius: 15, justifyContent: 'center', alignItems: 'center', marginTop: 20,borderWidth:2 },
  btnText: {  fontWeight: '800', fontSize: 16 },
});