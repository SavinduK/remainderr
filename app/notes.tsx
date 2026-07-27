import { FontAwesome5 } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Clipboard from 'expo-clipboard';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from "react";
import {
  Modal,
  Pressable,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
  useColorScheme
} from "react-native";
import { SafeAreaView } from 'react-native-safe-area-context';
import Footer from './footer';
import { Colors } from './theme';

export interface Note {
  id: string;
  title: string;
  content: string;
  createdAt: string;
  isFavorite: boolean;
}

const NOTES_KEY = 'notesKey';

export default function NotesScreen() {
  const [notes, setNotes] = useState<Note[]>([]);
  const [activeTab, setActiveTab] = useState<'all' | 'favorites'>('all');
  const [searchQuery, setSearchQuery] = useState('');
  
  // Menu Modal State
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [menuVisible, setMenuVisible] = useState(false);

  // Delete Modal State
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);

  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();

  const loadNotes = async () => {
    try {
      const data = await AsyncStorage.getItem(NOTES_KEY);
      if (data) {
        const parsed: Note[] = JSON.parse(data);
        // Sort latest notes on top
        parsed.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setNotes(parsed);
      }
    } catch (e) {
      console.error(e);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadNotes();
    }, [])
  );

  const saveNotesToStorage = async (updatedNotes: Note[]) => {
    setNotes(updatedNotes);
    await AsyncStorage.setItem(NOTES_KEY, JSON.stringify(updatedNotes));
  };

  const handleCopyNote = async () => {
    if (selectedNote) {
      const textToCopy = `${selectedNote.title}\n\n${selectedNote.content}`;
      await Clipboard.setStringAsync(textToCopy);
      setMenuVisible(false);
    }
  };

  const handleDeleteNote = async () => {
    if (selectedNote) {
      const filtered = notes.filter(n => n.id !== selectedNote.id);
      await saveNotesToStorage(filtered);
      setDeleteModalVisible(false);
      setMenuVisible(false);
    }
  };

  const formatRelativeTime = (isoDate: string) => {
    const date = new Date(isoDate);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return 'Just now';
    if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} hour ago`;
    if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`;
    return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  // Filter & Search Notes
  const filteredNotes = notes.filter(note => {
    const matchesTab = activeTab === 'all' ? true : note.isFavorite;
    const matchesSearch = note.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          note.content.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesTab && matchesSearch;
  });

  // Distribute into 2 columns for a masonry style
  const leftColumn = filteredNotes.filter((_, index) => index % 2 === 0);
  const rightColumn = filteredNotes.filter((_, index) => index % 2 !== 0);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      {/* HEADER WITH TITLE ON LEFT AND ADD BUTTON ON RIGHT */}
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: theme.title }]}>My Notes</Text>
        
        <View style={styles.headerActions}>
          <Pressable 
            style={[styles.addBtn, { backgroundColor: theme.accent }]} 
            onPress={() => router.push('/addnote')}
          >
            <FontAwesome5 name="plus" size={16} color="white" />
          </Pressable>
        </View>
      </View>

      {/* SEARCH BAR */}
      <View style={[styles.searchContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
        <FontAwesome5 name="search" size={16} color={theme.subtext} style={{ marginRight: 10 }} />
        <TextInput
          placeholder="Search"
          placeholderTextColor={theme.subtext}
          style={[styles.searchInput, { color: theme.title }]}
          value={searchQuery}
          onChangeText={setSearchQuery}
        />
      </View>

      {/* TABS WITH SPACE-AROUND ALIGNMENT */}
      <View style={styles.tabContainer}>
        <Pressable onPress={() => setActiveTab('all')} style={styles.tabButton}>
          <Text style={[styles.tabText, { color: activeTab === 'all' ? theme.accent : theme.subtext, fontWeight: activeTab === 'all' ? '700' : '500' }]}>
            All Notes
          </Text>
          {activeTab === 'all' && <View style={[styles.tabIndicator, { backgroundColor: theme.accent }]} />}
        </Pressable>

        <Pressable onPress={() => setActiveTab('favorites')} style={styles.tabButton}>
          <Text style={[styles.tabText, { color: activeTab === 'favorites' ? theme.accent : theme.subtext, fontWeight: activeTab === 'favorites' ? '700' : '500' }]}>
            Favorites
          </Text>
          {activeTab === 'favorites' && <View style={[styles.tabIndicator, { backgroundColor: theme.accent }]} />}
        </Pressable>
      </View>

      {/* NOTES GRID */}
      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
        {filteredNotes.length === 0 ? (
          <View style={styles.emptyContainer}>
            <FontAwesome5 name="sticky-note" size={50} color={theme.border} />
            <Text style={[styles.emptyText, { color: theme.subtext }]}>No notes found</Text>
          </View>
        ) : (
          <View style={styles.gridContainer}>
            {/* LEFT COLUMN */}
            <View style={styles.column}>
              {leftColumn.map(item => (
                <NoteCard
                  key={item.id}
                  item={item}
                  theme={theme}
                  timeLabel={formatRelativeTime(item.createdAt)}
                  onOpenMenu={(note:any) => {
                    setSelectedNote(note);
                    setMenuVisible(true);
                  }}
                />
              ))}
            </View>

            {/* RIGHT COLUMN */}
            <View style={styles.column}>
              {rightColumn.map(item => (
                <NoteCard
                  key={item.id}
                  item={item}
                  theme={theme}
                  timeLabel={formatRelativeTime(item.createdAt)}
                  onOpenMenu={(note:any) => {
                    setSelectedNote(note);
                    setMenuVisible(true);
                  }}
                />
              ))}
            </View>
          </View>
        )}
      </ScrollView>

      <Footer />

      {/* CONTEXT DROPDOWN MENU MODAL */}
      <Modal transparent visible={menuVisible} animationType="fade" onRequestClose={() => setMenuVisible(false)}>
        <Pressable style={styles.modalOverlay} onPress={() => setMenuVisible(false)}>
          <View style={[styles.menuContainer, { backgroundColor: theme.card, borderColor: theme.border }]}>
            <Pressable 
              style={styles.menuOption} 
              onPress={() => {
                setMenuVisible(false);
                if (selectedNote) router.push({ pathname: '/addnote', params: { id: selectedNote.id } });
              }}
            >
              <FontAwesome5 name="edit" size={16} color={theme.title} style={{ marginRight: 12 }} />
              <Text style={[styles.menuOptionText, { color: theme.title }]}>Edit</Text>
            </Pressable>

            <View style={[styles.divider, { backgroundColor: theme.border }]} />

            <Pressable style={styles.menuOption} onPress={handleCopyNote}>
              <FontAwesome5 name="copy" size={16} color={theme.title} style={{ marginRight: 12 }} />
              <Text style={[styles.menuOptionText, { color: theme.title }]}>Copy</Text>
            </Pressable>

            <View style={[styles.divider, { backgroundColor: theme.border }]} />

            <Pressable style={styles.menuOption} onPress={() => setDeleteModalVisible(true)}>
              <FontAwesome5 name="trash" size={16} color={theme.delete} style={{ marginRight: 12 }} />
              <Text style={[styles.menuOptionText, { color: theme.delete }]}>Delete</Text>
            </Pressable>
          </View>
        </Pressable>
      </Modal>

      {/* CONFIRM DELETE MODAL */}
      <Modal transparent visible={deleteModalVisible} animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Delete Note</Text>
            <Text style={[styles.modalSub, { color: theme.subtext }]}>Are you sure you want to delete this note?</Text>
            <View style={styles.modalActions}>
              <Pressable style={styles.modalBtn} onPress={() => setDeleteModalVisible(false)}>
                <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
              </Pressable>
              <Pressable style={[styles.modalBtn, { backgroundColor: theme.delete }]} onPress={handleDeleteNote}>
                <Text style={{ color: 'white', fontWeight: '600' }}>Delete</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

// NOTE CARD COMPONENT (No direct click-to-edit)
const NoteCard = ({ item, theme, timeLabel, onOpenMenu }: any) => {
  return (
    <View style={[styles.noteCard, { backgroundColor: theme.card, borderColor: theme.border }]}>
      <View style={styles.noteHeader}>
        <Text style={[styles.noteTime, { color: theme.subtext }]}>{timeLabel}</Text>
        <Pressable hitSlop={10} onPress={() => onOpenMenu(item)}>
          <FontAwesome5 name="ellipsis-v" size={14} color={theme.subtext} />
        </Pressable>
      </View>
      
      <Text style={[styles.noteTitle, { color: theme.title }]} numberOfLines={2}>
        {item.title}
      </Text>
      
      <Text style={[styles.noteContent, { color: theme.subtext }]} numberOfLines={6}>
        {item.content}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    alignItems: 'center', 
    paddingHorizontal: 25, 
    paddingTop: 15, 
    paddingBottom: 15 
  },
  headerTitle: { fontSize: 28, fontWeight: '800' },
  headerActions: { flexDirection: 'row', alignItems: 'center' },
  addBtn: { 
    width: 44, 
    height: 44, 
    borderRadius: 22, 
    justifyContent: 'center', 
    alignItems: 'center' 
  },
  searchContainer: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    marginHorizontal: 20, 
    paddingHorizontal: 15, 
    height: 46, 
    borderRadius: 23, 
    borderWidth: 1, 
    marginBottom: 15 
  },
  searchInput: { flex: 1, fontSize: 15 },
  tabContainer: { 
    flexDirection: 'row', 
    justifyContent: 'space-around', 
    paddingHorizontal: 15, 
    marginBottom: 15 
  },
  tabButton: { paddingBottom: 6, position: 'relative', alignItems: 'center' },
  tabText: { fontSize: 15 },
  tabIndicator: { position: 'absolute', bottom: 0, left: 0, right: 0, height: 3, borderRadius: 2 },
  scroll: { flex: 1, paddingHorizontal: 15 },
  gridContainer: { flexDirection: 'row', gap: 12 },
  column: { flex: 1, gap: 12 },
  noteCard: { padding: 16, borderRadius: 16, borderWidth: 1 },
  noteHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  noteTime: { fontSize: 11, fontWeight: '500' },
  noteTitle: { fontSize: 16, fontWeight: '700', marginBottom: 6 },
  noteContent: { fontSize: 13, lineHeight: 18 },
  emptyContainer: { alignItems: 'center', marginTop: 100 },
  emptyText: { marginTop: 15, fontSize: 16, fontWeight: '500' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  menuContainer: { width: 180, borderRadius: 16, borderWidth: 1, paddingVertical: 6, elevation: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.15, shadowRadius: 8 },
  menuOption: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12 },
  menuOptionText: { fontSize: 15, fontWeight: '600' },
  divider: { height: 1, width: '100%' },
  modalContent: { width: '85%', padding: 25, borderRadius: 25, alignItems: 'center' },
  modalTitle: { fontSize: 20, fontWeight: '700', marginBottom: 10 },
  modalSub: { textAlign: 'center', marginBottom: 25 },
  modalActions: { flexDirection: 'row', gap: 15 },
  modalBtn: { flex: 1, padding: 15, borderRadius: 15, alignItems: 'center' }
});