import { FontAwesome5, Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Clipboard from 'expo-clipboard';
import { useFocusEffect, useRouter } from 'expo-router';
import React, { useCallback, useState } from 'react';
import { FlatList, Modal, Pressable, StyleSheet, Text, TouchableOpacity, useColorScheme, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import AddPasswordModal from './addPassword';
import Footer from './footer';
import { Colors } from './theme';

const STORAGE_KEY = 'user_passwords_list';

// Helper function to resolve FontAwesome icons for common platforms
const getSiteIcon = (siteName: string) => {
  const name = siteName ? siteName.toLowerCase().trim() : '';

  if (name.includes('paypal')) return { name: 'paypal', color: '#003087', brand: true };
  if (name.includes('facebook')) return { name: 'facebook', color: '#1877F2', brand: true };
  if (name.includes('instagram')) return { name: 'instagram', color: '#E4405F', brand: true };
  if (name.includes('debit') || name.includes('credit') || name.includes('visa')) return { name: 'cc-visa', color: '#1A1F71', brand: true };
  if (name.includes('mastercard')) return { name: 'cc-mastercard', color: '#EB001B', brand: true };
  if (name.includes('google')) return { name: 'google', color: '#4285F4', brand: true };
  if (name.includes('twitter') || name.includes('x')) return { name: 'twitter', color: '#1DA1F2', brand: true };
  if (name.includes('github')) return { name: 'github', color: '#7e7b7b', brand: true };
  if (name.includes('apple')) return { name: 'apple', color: '#000000', brand: true };

  // Fallback icon for uncommon apps/sites
  return { name: 'key', color: '#6C757D', brand: false };
};

export default function VaultScreen() {
  const router = useRouter();
  const colorScheme = useColorScheme() ?? 'light';
  const theme = Colors[colorScheme];
  
  const [passwords, setPasswords] = useState<any[]>([]);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isAddModalVisible, setAddModalVisible] = useState(false);

  // State to track password visibility per item
  const [visiblePasswordIds, setVisiblePasswordIds] = useState<{ [key: string]: boolean }>({});
  
  // State to track active action menu dropdown
  const [activeMenuId, setActiveMenuId] = useState<string | null>(null);

  useFocusEffect(
    useCallback(() => {
      loadPasswords();
    }, [])
  );

  const loadPasswords = async () => {
    try {
      const result = await AsyncStorage.getItem(STORAGE_KEY);
      if (result) setPasswords(JSON.parse(result));
    } catch (e) {
      console.error("Could not load the vault.");
    }
  };

  const togglePasswordVisibility = (id: string) => {
    setVisiblePasswordIds(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const confirmDelete = (id: string) => {
    setSelectedId(id);
    setActiveMenuId(null);
    setDeleteModalVisible(true);
  };

  const deleteEntry = async () => {
    if (!selectedId) return;
    
    const updatedList = passwords.filter((item: any) => item.id !== selectedId);
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(updatedList));
    setPasswords(updatedList);
    setDeleteModalVisible(false);
    setSelectedId(null);
  };

  const copyToClipboard = async (text: string, label: string) => {
    await Clipboard.setStringAsync(text);
    console.log(`${label} copied`); 
    setActiveMenuId(null);
  };

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: theme.background }]}>
      <AddPasswordModal 
        visible={isAddModalVisible} 
        onClose={() => setAddModalVisible(false)} 
        onSave={loadPasswords} 
      />

      {/* Delete Confirmation Modal */}
      <Modal
        transparent
        visible={deleteModalVisible}
        animationType="fade"
        onRequestClose={() => setDeleteModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: theme.card, borderColor: theme.border }]}>
            <Text style={[styles.modalTitle, { color: theme.title }]}>Delete Credential?</Text>
            <Text style={[styles.modalSubtext, { color: theme.subtext }]}>
              This action cannot be undone.
            </Text>
            
            <View style={styles.modalActionRow}>
              <TouchableOpacity 
                style={[styles.modalBtn, { backgroundColor: 'transparent' }]} 
                onPress={() => setDeleteModalVisible(false)}
              >
                <Text style={{ color: theme.subtext, fontWeight: '600' }}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity 
                style={[styles.modalBtn, { backgroundColor: theme.delete + '20' }]} 
                onPress={deleteEntry}
              >
                <Text style={{ color: theme.delete, fontWeight: '700' }}>Delete</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Header */}
      <View style={styles.header}>
          <Text style={[styles.headerTitle, { color: theme.title }]}>Passwords</Text>
        <Pressable style={[styles.addBtn, { backgroundColor: theme.accent }]} onPress={() => setAddModalVisible(true)}>
          <FontAwesome5 name="plus" size={16} color={theme.title} />
        </Pressable>
      </View>

      {/* Vault List */}
      <FlatList
        data={passwords}
        keyExtractor={(item: any) => item.id}
        contentContainerStyle={styles.listContent}
        renderItem={({ item }) => {
          const iconConfig = getSiteIcon(item.site);
          const isPasswordShown = !!visiblePasswordIds[item.id];
          const isMenuOpen = activeMenuId === item.id;

          return (
            <View style={styles.cardContainer}>
              <View style={[styles.card, { backgroundColor: theme.card, borderColor: theme.border }]}>
                {/* Brand / App Icon */}
                <View style={styles.iconWrapper}>
                  <FontAwesome5 
                    name={iconConfig.name} 
                    size={28} 
                    color={iconConfig.color} 
                    solid={!iconConfig.brand}
                  />
                </View>

                {/* Main Details (Site & Masked Password) */}
                <View style={styles.cardTextContainer}>
                  <Text style={[styles.siteName, { color: theme.title }]} numberOfLines={1}>
                    {item.site}
                  </Text>
                  
                  <Text style={[styles.passwordText, { color: theme.title }]}>
                    {isPasswordShown ? item.password : '●●●●●●●●'}
                  </Text>
                </View>

                {/* Actions: Eye Icon & 3-Dots Menu */}
                <View style={styles.cardActions}>
                  <TouchableOpacity 
                    onPress={() => togglePasswordVisibility(item.id)} 
                    style={styles.actionIconButton}
                  >
                    <Ionicons 
                      name={isPasswordShown ? "eye-off-outline" : "eye-outline"} 
                      size={22} 
                      color={theme.title} 
                    />
                  </TouchableOpacity>

                  <TouchableOpacity 
                    onPress={() => setActiveMenuId(isMenuOpen ? null : item.id)} 
                    style={styles.actionIconButton}
                  >
                    <Ionicons name="ellipsis-vertical" size={20} color={theme.title} />
                  </TouchableOpacity>
                </View>
              </View>

              {/* Contextual Action Dropdown */}
              {isMenuOpen && (
                <View style={[styles.dropdownMenu, { backgroundColor: theme.card, borderColor: theme.border }]}>
                  <TouchableOpacity 
                    style={styles.dropdownItem} 
                    onPress={() => copyToClipboard(item.username, "Username")}
                  >
                    <Ionicons name="person-outline" size={16} color={theme.title} />
                    <Text style={[styles.dropdownText, { color: theme.title }]}>Copy Username</Text>
                  </TouchableOpacity>

                  <TouchableOpacity 
                    style={styles.dropdownItem} 
                    onPress={() => copyToClipboard(item.password, "Password")}
                  >
                    <Ionicons name="copy-outline" size={16} color={theme.title} />
                    <Text style={[styles.dropdownText, { color: theme.title }]}>Copy Password</Text>
                  </TouchableOpacity>

                  <View style={[styles.dropdownDivider, { backgroundColor: theme.border }]} />

                  <TouchableOpacity 
                    style={styles.dropdownItem} 
                    onPress={() => confirmDelete(item.id)}
                  >
                    <Ionicons name="trash-outline" size={16} color={theme.delete} />
                    <Text style={[styles.dropdownText, { color: theme.delete }]}>Delete</Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>
          );
        }}
      />
      <Footer/>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingTop: 15, paddingBottom: 25 },
  headerSubtitle: { fontSize: 13, fontWeight: '600', opacity: 0.7 },
  headerTitle: { fontSize: 26, fontWeight: '800', letterSpacing: -0.5, marginTop: 2 },
  addBtn: { width: 44, height: 44, borderRadius: 22, justifyContent: 'center', alignItems: 'center' },
  listContent: { paddingHorizontal: 20, paddingBottom: 40 },
  cardContainer: {
    marginBottom: 12,
    position: 'relative',
    zIndex: 1,
  },
  card: { 
    flexDirection: 'row', 
    alignItems: 'center', 
    paddingHorizontal: 16, 
    paddingVertical: 14,
    borderRadius: 20, 
    borderWidth: 1,
  },
  iconWrapper: {
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  cardTextContainer: { 
    flex: 1,
    justifyContent: 'center',
  },
  siteName: { 
    fontSize: 16, 
    fontWeight: '700', 
    marginBottom: 4 
  },
  passwordText: { 
    fontSize: 13, 
    fontWeight: '800', 
    letterSpacing: 1.5 
  },
  cardActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  actionIconButton: {
    padding: 6,
  },

  // Dropdown Styling
  dropdownMenu: {
    position: 'absolute',
    right: 10,
    top: 55,
    borderRadius: 14,
    borderWidth: 1,
    paddingVertical: 6,
    width: 170,
    zIndex: 999,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
  },
  dropdownItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
    paddingVertical: 10,
    gap: 10,
  },
  dropdownText: {
    fontSize: 13,
    fontWeight: '600',
  },
  dropdownDivider: {
    height: 1,
    marginVertical: 4,
  },

  // Modal Styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalContent: {
    width: '100%',
    maxWidth: 340,
    borderRadius: 24,
    padding: 24,
    alignItems: 'center',
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: '800',
    marginBottom: 8,
  },
  modalSubtext: {
    fontSize: 15,
    textAlign: 'center',
    marginBottom: 24,
  },
  modalActionRow: {
    flexDirection: 'row',
    gap: 12,
  },
  modalBtn: {
    flex: 1,
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
});