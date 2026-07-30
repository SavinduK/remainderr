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
  isCompleted?: boolean;
  category?: string;
}

const NOTES_KEY = 'notesKey';
const CATEGORIES_KEY = 'categoriesKey';
const DEFAULT_CATEGORIES = ['Work', 'Personal', 'Ideas', 'To-Do'];

export default function NotesScreen() {
  const [notes, setNotes] = useState<Note[]>([]);
  const [categories, setCategories] = useState<string[]>(DEFAULT_CATEGORIES);
  
  // Navigation Tabs: 'all' | 'favorites' | 'categories'
  const [activeTab, setActiveTab] = useState<'all' | 'favorites' | 'categories'>('all');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Category Edit State inside Modal
  const [isEditMode, setIsEditMode] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  
  // Context Menu Modal State
  const [selectedNote, setSelectedNote] = useState<Note | null>(null);
  const [menuVisible, setMenuVisible] = useState(false);

  // Category Picker / Management Modal State
  const [categoryModalVisible, setCategoryModalVisible] = useState(false);

  // Delete Note Modal State
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);

  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];
  const router = useRouter();

  // Load Notes & Categories
  const loadData = async () => {
    try {
      const notesData = await AsyncStorage.getItem(NOTES_KEY);
      if (notesData) {
        setNotes(JSON.parse(notesData));
      }

      const categoriesData = await AsyncStorage.getItem(CATEGORIES_KEY);
      if (categoriesData) {
        setCategories(JSON.parse(categoriesData));
      } else {
        await AsyncStorage.setItem(CATEGORIES_KEY, JSON.stringify(DEFAULT_CATEGORIES));
      }
    } catch (e) {
      console.error(e);
    }
  };

  useFocusEffect(
    useCallback(() => {
      loadData();
    }, [])
  );

  const saveNotesToStorage = async (updatedNotes: Note[]) => {
    setNotes(updatedNotes);
    await AsyncStorage.setItem(NOTES_KEY, JSON.stringify(updatedNotes));
  };

  const saveCategoriesToStorage = async (updatedCategories: string[]) => {
    setCategories(updatedCategories);
    await AsyncStorage.setItem(CATEGORIES_KEY, JSON.stringify(updatedCategories));
  };

  // --- Category Actions ---
  const handleAddCategory = async () => {
    if (!newCategoryName.trim()) return;
    const formatted = newCategoryName.trim();
    if (!categories.includes(formatted)) {
      const updated = [...categories, formatted];
      await saveCategoriesToStorage(updated);
    }
    setNewCategoryName('');
  };

  const handleDeleteCategory = async (catToDelete: string) => {
    const updated = categories.filter(c => c !== catToDelete);
    await saveCategoriesToStorage(updated);

    // Unassign category from existing notes
    const updatedNotes = notes.map(n => n.category === catToDelete ? { ...n, category: undefined } : n);
    await saveNotesToStorage(updatedNotes);

    if (selectedCategory === catToDelete) {
      setSelectedCategory(null);
    }
  };

  const handleSetNoteCategory = async (category: string) => {
    if (selectedNote) {
      const updated = notes.map(n => 
        n.id === selectedNote.id ? { ...n, category } : n
      );
      await saveNotesToStorage(updated);
      setCategoryModalVisible(false);
      setSelectedNote(null);
      setIsEditMode(false);
    }
  };

  // --- Note Actions ---
  const handleToggleFavorite = async (noteToToggle: Note) => {
    const updated = notes.map(n => 
      n.id === noteToToggle.id ? { ...n, isFavorite: !n.isFavorite } : n
    );
    await saveNotesToStorage(updated);
  };

  const handleToggleComplete = async () => {
    if (selectedNote) {
      const updated = notes.map(n => 
        n.id === selectedNote.id ? { ...n, isCompleted: !n.isCompleted } : n
      );
      await saveNotesToStorage(updated);
      setMenuVisible(false);
    }
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

  // Filter Notes Logic
  const filteredNotes = notes
    .filter(note => {
      let matchesTab = true;
      if (activeTab === 'favorites') {
        matchesTab = note.isFavorite;
      } else if (activeTab === 'categories') {
        matchesTab = selectedCategory ? note.category === selectedCategory : false;
      }

      const matchesSearch = note.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                            note.content.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesTab && matchesSearch;
    })
    .sort((a, b) => {
      if (!!a.isCompleted !== !!b.isCompleted) {
        return a.isCompleted ? 1 : -1;
      }
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });

  const leftColumn = filteredNotes.filter((_, index) => index % 2 === 0);
  const rightColumn = filteredNotes.filter((_, index) => index % 2 !== 0);

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.background }]}>
      <StatusBar barStyle={colorScheme === 'dark' ? "light-content" : "dark-content"} />

      {/* HEADER */}
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

      {/* MAIN NAVIGATION TABS */}
      <View style={styles.tabContainer}>
        <Pressable onPress={() => { setActiveTab('all'); setSelectedCategory(null); }} style={styles.tabButton}>
          <Text style={[styles.tabText, { color: activeTab === 'all' ? theme.accent : theme.subtext, fontWeight: activeTab === 'all' ? '700' : '500' }]}>
            All Notes
          </Text>
          {activeTab === 'all' && <View style={[styles.tabIndicator, { backgroundColor: theme.accent }]} />}
        </Pressable>

        <Pressable onPress={() => { setActiveTab('favorites'); setSelectedCategory(null); }} style={styles.tabButton}>
          <Text style={[styles.tabText, { color: activeTab === 'favorites' ? theme.accent : theme.subtext, fontWeight: activeTab === 'favorites' ? '700' : '500' }]}>
            Favorites
          </Text>
          {activeTab === 'favorites' && <View style={[styles.tabIndicator, { backgroundColor: theme.accent }]} />}
        </Pressable>

        <Pressable onPress={() => setActiveTab('categories')} style={styles.tabButton}>
          <Text style={[styles.tabText, { color: activeTab === 'categories' ? theme.accent : theme.subtext, fontWeight: activeTab === 'categories' ? '700' : '500' }]}>
            Categories
          </Text>
          {activeTab === 'categories' && <View style={[styles.tabIndicator, { backgroundColor: theme.accent }]} />}
        </Pressable>
      </View>

      {/* MAIN SCROLL VIEW */}
      <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 100 }}>
        
        {/* CATEGORY SELECTION HEADER (If a Category is active) */}
        {activeTab === 'categories' && selectedCategory && (
          <View style={styles.categoryHeaderFilterBar}>
            <Text style={[styles.categoryActiveTitle, { color: theme.title }]}>
              Category: <Text style={{ color: theme.accent }}>{selectedCategory}</Text>
            </Text>
            <Pressable 
              style={[styles.clearCategoryBtn, { backgroundColor: theme.card, borderColor: theme.border }]} 
              onPress={() => setSelectedCategory(null)}
            >
              <FontAwesome5 name="arrow-left" size={12} color={theme.subtext} style={{ marginRight: 6 }} />
              <Text style={{ fontSize: 13, color: theme.subtext, fontWeight: '600' }}>All Categories</Text>
            </Pressable>
          </View>
        )}

        {/* 100% WIDTH CATEGORY CARDS (Shown when inside Categories tab AND no category is selected) */}
        {activeTab === 'categories' && !selectedCategory && (
          <View style={styles.fullWidthCategoryList}>
            {categories.map(cat => {
              const noteCount = notes.filter(n => n.category === cat).length;

              return (
                <Pressable
                  key={cat}
                  onPress={() => setSelectedCategory(cat)}
                  style={[
                    styles.fullWidthCategoryCard,
                    {
                      backgroundColor: theme.card,
                      borderColor: theme.border,
                    }
                  ]}
                >
                  <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                    <FontAwesome5 name="folder" size={20} color={theme.accent} style={{ marginRight: 14 }} />
                    <Text style={[styles.fullWidthCategoryTitle, { color: theme.title }]}>
                      {cat}
                    </Text>
                  </View>
                  <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                    <Text style={[styles.fullWidthCategoryCount, { color: theme.subtext }]}>
                      {noteCount} {noteCount === 1 ? 'note' : 'notes'}
                    </Text>
                    <FontAwesome5 name="chevron-right" size={12} color={theme.subtext} style={{ marginLeft: 10 }} />
                  </View>
                </Pressable>
              );
            })}
          </View>
        )}

        {/* NOTES MASONRY GRID (Only renders when NOT in category selection list) */}
        {(activeTab !== 'categories' || selectedCategory) && (
          filteredNotes.length === 0 ? (
            <View style={styles.emptyContainer}>
              <FontAwesome5 name="sticky-note" size={50} color={theme.border} />
              <Text style={[styles.emptyText, { color: theme.subtext }]}>
                {activeTab === 'categories' ? `No notes in "${selectedCategory}"` : "No notes found"}
              </Text>
            </View>
          ) : (
            <View style={styles.gridContainer}>
              <View style={styles.column}>
                {leftColumn.map(item => (
                  <NoteCard
                    key={item.id}
                    item={item}
                    theme={theme}
                    timeLabel={formatRelativeTime(item.createdAt)}
                    onToggleFavorite={() => handleToggleFavorite(item)}
                    onOpenMenu={(note: Note) => {
                      setSelectedNote(note);
                      setMenuVisible(true);
                    }}
                  />
                ))}
              </View>

              <View style={styles.column}>
                {rightColumn.map(item => (
                  <NoteCard
                    key={item.id}
                    item={item}
                    theme={theme}
                    timeLabel={formatRelativeTime(item.createdAt)}
                    onToggleFavorite={() => handleToggleFavorite(item)}
                    onOpenMenu={(note: Note) => {
                      setSelectedNote(note);
                      setMenuVisible(true);
                    }}
                  />
                ))}
              </View>
            </View>
          )
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
              <Text style={[styles.menuOptionText, { color: theme.title }]}>Edit Note</Text>
            </Pressable>

            <View style={[styles.divider, { backgroundColor: theme.border }]} />

            <Pressable 
              style={styles.menuOption} 
              onPress={() => {
                setMenuVisible(false);
                setIsEditMode(false);
                setCategoryModalVisible(true);
              }}
            >
              <FontAwesome5 name="tag" size={16} color={theme.title} style={{ marginRight: 12 }} />
              <Text style={[styles.menuOptionText, { color: theme.title }]}>Category</Text>
            </Pressable>

            <View style={[styles.divider, { backgroundColor: theme.border }]} />

            <Pressable style={styles.menuOption} onPress={handleToggleComplete}>
              <FontAwesome5 
                name={selectedNote?.isCompleted ? "undo" : "check-circle"} 
                size={16} 
                color={theme.title} 
                style={{ marginRight: 12 }} 
              />
              <Text style={[styles.menuOptionText, { color: theme.title }]}>
                {selectedNote?.isCompleted ? "Mark Incomplete" : "Mark Complete"}
              </Text>
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

      {/* CATEGORY PICKER & EDIT MODAL */}
      <Modal transparent visible={categoryModalVisible} animationType="fade" onRequestClose={() => setCategoryModalVisible(false)}>
        <Pressable style={styles.modalOverlay} onPress={() => setCategoryModalVisible(false)}>
          <Pressable style={[styles.modalContent, { backgroundColor: theme.card }]}>
            <View style={styles.modalHeaderRow}>
              <Text style={[styles.modalTitle, { color: theme.title }]}>Select Category</Text>
              
              {/* EDIT MODE TOGGLE BUTTON */}
              <Pressable hitSlop={10} onPress={() => setIsEditMode(!isEditMode)}>
                <FontAwesome5 name="edit" size={18} color={isEditMode ? theme.accent : theme.subtext} />
              </Pressable>
            </View>

            {/* ADD NEW CATEGORY INPUT ROW (ONLY SHOWN IN EDIT MODE) */}
            {isEditMode && (
              <View style={styles.addCategoryRow}>
                <TextInput
                  placeholder="New category name"
                  placeholderTextColor={theme.subtext}
                  style={[styles.addCategoryInput, { color: theme.title, borderColor: theme.border }]}
                  value={newCategoryName}
                  onChangeText={setNewCategoryName}
                />
                <Pressable style={[styles.addCategoryBtn, { backgroundColor: theme.accent }]} onPress={handleAddCategory}>
                  <FontAwesome5 name="plus" size={12} color="white" style={{ marginRight: 4 }} />
                  <Text style={{ color: 'white', fontWeight: '700', fontSize: 13 }}>Add</Text>
                </Pressable>
              </View>
            )}

            {/* CATEGORY LIST (DELETE ACTION ONLY SHOWN IN EDIT MODE) */}
            <ScrollView style={{ width: '100%', maxHeight: 220, marginVertical: 10 }}>
              {categories.map((cat) => (
                <View key={cat} style={[styles.categoryOptionRow, { borderColor: theme.border }]}>
                  <Pressable
                    style={{ flex: 1, flexDirection: 'row', alignItems: 'center' }}
                    onPress={() => handleSetNoteCategory(cat)}
                  >
                    <Text style={{ fontSize: 15, fontWeight: '600', color: selectedNote?.category === cat ? theme.accent : theme.title }}>
                      {cat}
                    </Text>
                    {selectedNote?.category === cat && (
                      <FontAwesome5 name="check" size={14} color={theme.accent} style={{ marginLeft: 8 }} />
                    )}
                  </Pressable>

                  {/* DELETE BUTTON (ONLY VISIBLE IN EDIT MODE) */}
                  {isEditMode && (
                    <Pressable hitSlop={10} onPress={() => handleDeleteCategory(cat)}>
                      <FontAwesome5 name="trash-alt" size={14} color={theme.delete} />
                    </Pressable>
                  )}
                </View>
              ))}
            </ScrollView>

            <Pressable style={[styles.modalBtn, { marginTop: 5 }]} onPress={() => setCategoryModalVisible(false)}>
              <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
            </Pressable>
          </Pressable>
        </Pressable>
      </Modal>

      {/* DELETE CONFIRM MODAL */}
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

// NOTE CARD COMPONENT
const NoteCard = ({ item, theme, timeLabel, onToggleFavorite, onOpenMenu }: any) => {
  return (
    <View style={[
      styles.noteCard, 
      { backgroundColor: theme.card, borderColor: theme.border, opacity: item.isCompleted ? 0.6 : 1 }
    ]}>
      <View style={styles.noteHeader}>
        <Text style={[styles.noteTime, { color: theme.subtext }]}>{timeLabel}</Text>
        
        <View style={styles.cardHeaderActions}>
          <Pressable hitSlop={10} onPress={onToggleFavorite} style={{ marginRight: 12 }}>
            <FontAwesome5 
              name="bookmark" 
              size={14} 
              solid={item.isFavorite} 
              color={item.isFavorite ? theme.accent : theme.subtext} 
            />
          </Pressable>
          <Pressable hitSlop={10} onPress={() => onOpenMenu(item)}>
            <FontAwesome5 name="ellipsis-v" size={14} color={theme.subtext} />
          </Pressable>
        </View>
      </View>
      
      <Text 
        style={[
          styles.noteTitle, 
          { color: theme.title, textDecorationLine: item.isCompleted ? 'line-through' : 'none' }
        ]} 
        numberOfLines={2}
      >
        {item.title}
      </Text>
      
      <Text 
        style={[
          styles.noteContent, 
          { color: theme.subtext, textDecorationLine: item.isCompleted ? 'line-through' : 'none' }
        ]} 
      >
        {item.content}
      </Text>

      {item.category && (
        <View style={[styles.badge, { backgroundColor: theme.accent + '20' }]}>
          <Text style={[styles.badgeText, { color: theme.accent }]}>{item.category}</Text>
        </View>
      )}
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
  
  // Category Selection Filter Bar
  categoryHeaderFilterBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
    paddingHorizontal: 5
  },
  categoryActiveTitle: { fontSize: 18, fontWeight: '700' },
  clearCategoryBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1
  },

  // 100% Width Category List Styles
  fullWidthCategoryList: { width: '100%', gap: 10, marginBottom: 15 },
  fullWidthCategoryCard: { 
    width: '100%', 
    paddingHorizontal: 18, 
    paddingVertical: 16, 
    borderRadius: 16, 
    borderWidth: 1, 
    flexDirection: 'row', 
    justifyContent: 'space-between', 
    alignItems: 'center' 
  },
  fullWidthCategoryTitle: { fontSize: 16, fontWeight: '700' },
  fullWidthCategoryCount: { fontSize: 13, fontWeight: '500' },

  scroll: { flex: 1, paddingHorizontal: 15 },
  gridContainer: { flexDirection: 'row', gap: 12 },
  column: { flex: 1, gap: 12 },
  noteCard: { padding: 16, borderRadius: 16, borderWidth: 1 },
  noteHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  cardHeaderActions: { flexDirection: 'row', alignItems: 'center' },
  noteTime: { fontSize: 11, fontWeight: '500' },
  noteTitle: { fontSize: 16, fontWeight: '700', marginBottom: 6 },
  noteContent: { fontSize: 13, lineHeight: 18 },
  badge: { alignSelf: 'flex-start', marginTop: 10, paddingHorizontal: 8, paddingVertical: 3, borderRadius: 8 },
  badgeText: { fontSize: 10, fontWeight: '700' },
  emptyContainer: { alignItems: 'center', marginTop: 50 },
  emptyText: { marginTop: 15, fontSize: 16, fontWeight: '500' },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'center', alignItems: 'center' },
  menuContainer: { width: 200, borderRadius: 16, borderWidth: 1, paddingVertical: 6, elevation: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.15, shadowRadius: 8 },
  menuOption: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 12 },
  menuOptionText: { fontSize: 15, fontWeight: '600' },
  divider: { height: 1, width: '100%' },
  modalContent: { width: '85%', padding: 25, borderRadius: 25, alignItems: 'center' },
  modalHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', width: '100%', marginBottom: 10 },
  modalTitle: { fontSize: 20, fontWeight: '700' },
  modalSub: { textAlign: 'center', marginBottom: 25 },
  modalActions: { flexDirection: 'row', gap: 15 },
  modalBtn: { flex: 1, padding: 15, borderRadius: 15, alignItems: 'center' },
  
  // Category Modal Options
  addCategoryRow: { flexDirection: 'row', width: '100%', gap: 8, marginVertical: 8 },
  addCategoryInput: { flex: 1, height: 40, borderWidth: 1, borderRadius: 10, paddingHorizontal: 12, fontSize: 14 },
  addCategoryBtn: { flexDirection: 'row', paddingHorizontal: 14, justifyContent: 'center', alignItems: 'center', borderRadius: 10 },
  categoryOptionRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', width: '100%', paddingVertical: 12, borderBottomWidth: 1 }
});