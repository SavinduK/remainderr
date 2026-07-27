import { FontAwesome5 } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from "react";
import {
    KeyboardAvoidingView,
    Platform,
    Pressable,
    ScrollView,
    StatusBar,
    StyleSheet,
    TextInput,
    View,
    useColorScheme
} from "react-native";
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from './theme';

interface Note {
  id: string;
  title: string;
  content: string;
  createdAt: string;
  isFavorite: boolean;
}

const NOTES_KEY = 'notesKey';

export default function AddNoteScreen() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [isFavorite, setIsFavorite] = useState(false);
  const [createdAt, setCreatedAt] = useState<string>(new Date().toISOString());

  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();

  useEffect(() => {
    if (id) {
      loadExistingNote(id);
    }
  }, [id]);

  const loadExistingNote = async (noteId: string) => {
    try {
      const data = await AsyncStorage.getItem(NOTES_KEY);
      if (data) {
        const notes: Note[] = JSON.parse(data);
        const existing = notes.find(n => n.id === noteId);
        if (existing) {
          setTitle(existing.title);
          setContent(existing.content);
          setIsFavorite(existing.isFavorite);
          setCreatedAt(existing.createdAt);
        }
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleSaveAndBack = async () => {
    if (title.trim() === '' && content.trim() === '') {
      router.back();
      return;
    }

    try {
      const data = await AsyncStorage.getItem(NOTES_KEY);
      let notes: Note[] = data ? JSON.parse(data) : [];

      if (id) {
        // Update existing note
        notes = notes.map(n => n.id === id ? { ...n, title, content, isFavorite, createdAt: new Date().toISOString() } : n);
      } else {
        // Create new note
        const newNote: Note = {
          id: Date.now().toString(),
          title: title || 'Untitled Note',
          content,
          createdAt: new Date().toISOString(),
          isFavorite
        };
        notes.unshift(newNote); // Prepend to top
      }

      await AsyncStorage.setItem(NOTES_KEY, JSON.stringify(notes));
      router.back();
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      {/* TOP BAR */}
      <View style={styles.header}>
        <Pressable onPress={handleSaveAndBack} hitSlop={10}>
          <FontAwesome5 name="arrow-left" size={18} color={theme.title} />
        </Pressable>

        <View style={styles.headerRight}>
          <Pressable onPress={() => setIsFavorite(!isFavorite)} style={{ marginRight: 20 }} hitSlop={10}>
            <FontAwesome5 
              name="bookmark" 
              size={18} 
              color={isFavorite ? theme.accent : theme.subtext} 
              solid={isFavorite}
            />
          </Pressable>
          <Pressable onPress={handleSaveAndBack} hitSlop={10}>
            <FontAwesome5 name="check" size={18} color={theme.accent} />
          </Pressable>
        </View>
      </View>

      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={{ flex: 1 }}
      >
        <ScrollView style={styles.contentContainer} showsVerticalScrollIndicator={false}>
          {/* TITLE INPUT */}
          <TextInput
            placeholder="Title"
            placeholderTextColor={theme.subtext}
            style={[styles.titleInput, { color: theme.title }]}
            value={title}
            onChangeText={setTitle}
            multiline
          />

          {/* CONTENT INPUT */}
          <TextInput
            placeholder="Tap to add text"
            placeholderTextColor={theme.subtext}
            style={[styles.contentInput, { color: theme.title }]}
            value={content}
            onChangeText={setContent}
            multiline
            textAlignVertical="top"
          />
        </ScrollView>

        {/* BOTTOM RICH EDIT TOOLBAR */}
        <View style={[styles.toolbar, { borderTopColor: theme.border }]}>
          <Pressable><FontAwesome5 name="image" size={18} color={theme.subtext} /></Pressable>
          <Pressable><FontAwesome5 name="font" size={18} color={theme.subtext} /></Pressable>
          <Pressable><FontAwesome5 name="bold" size={18} color={theme.subtext} /></Pressable>
          <Pressable><FontAwesome5 name="italic" size={18} color={theme.subtext} /></Pressable>
          <Pressable><FontAwesome5 name="underline" size={18} color={theme.subtext} /></Pressable>
          <Pressable><FontAwesome5 name="undo" size={18} color={theme.subtext} /></Pressable>
          <Pressable><FontAwesome5 name="redo" size={18} color={theme.subtext} /></Pressable>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 25, paddingTop: 15, paddingBottom: 15 },
  headerRight: { flexDirection: 'row', alignItems: 'center' },
  contentContainer: { flex: 1, paddingHorizontal: 25 },
  titleInput: { fontSize: 26, fontWeight: '700', marginBottom: 15, paddingVertical: 5 },
  contentInput: { fontSize: 16, lineHeight: 24, minHeight: 250 },
  toolbar: { flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center', paddingVertical: 14, borderTopWidth: 1 }
});