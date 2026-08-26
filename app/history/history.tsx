import { Ionicons } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useFocusEffect, useRouter } from 'expo-router';
import * as Sharing from 'expo-sharing';
import React, { useState } from 'react';
import {
  Alert,
  FlatList,
  Modal,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  TouchableWithoutFeedback,
  useColorScheme,
  View,
} from 'react-native';
import Footer from '../footer';
import { Colors } from '../theme';


const HISTORY_DIR = FileSystem.documentDirectory + 'histories/';
const colorScheme = useColorScheme();
const theme = Colors[colorScheme ?? 'light'];

interface FileItem {
  name: string;
  formattedDate: string;
}

export default function HistoryList() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [menuVisible, setMenuVisible] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ top: 0, right: 16 });

  const router = useRouter();

  useFocusEffect(
    React.useCallback(() => {
      loadFiles();
    }, [])
  );

  const formatDate = (timestamp?: number) => {
    if (!timestamp) return '';
    const date = new Date(timestamp * 1000);
    const month = date.toLocaleString('en-US', { month: 'long' });
    const day = date.getDate();
    return `${month} ${day}`;
  };

  const loadFiles = async () => {
    const dirInfo = await FileSystem.getInfoAsync(HISTORY_DIR);
    if (!dirInfo.exists) {
      await FileSystem.makeDirectoryAsync(HISTORY_DIR, { intermediates: true });
    }
    const dirFiles = await FileSystem.readDirectoryAsync(HISTORY_DIR);
    const jsonFiles = dirFiles.filter((f) => f.endsWith('.json'));

    const fileDetails = await Promise.all(
      jsonFiles.map(async (fileName) => {
        const info = await FileSystem.getInfoAsync(HISTORY_DIR + fileName);
        return {
          name: fileName,
          formattedDate: info.exists ? formatDate(info.modificationTime) : '',
        };
      })
    );

    setFiles(fileDetails);
  };

  const openMenu = (fileName: string, targetRef: View | null) => {
    if (targetRef) {
      targetRef.measure((x, y, width, height, pageX, pageY) => {
        setMenuPosition({
          top: pageY + height + 4,
          right: 16,
        });
        setSelectedFile(fileName);
        setMenuVisible(true);
      });
    } else {
      setSelectedFile(fileName);
      setMenuVisible(true);
    }
  };

  const closeMenu = () => {
    setMenuVisible(false);
    setSelectedFile(null);
  };

  const shareFile = async () => {
    if (!selectedFile) return;
    const fileUri = HISTORY_DIR + selectedFile;
    const isAvailable = await Sharing.isAvailableAsync();

    closeMenu();

    if (!isAvailable) {
      Alert.alert('Error', 'Sharing is not available on this device.');
      return;
    }

    try {
      await Sharing.shareAsync(fileUri, {
        mimeType: 'application/json',
        dialogTitle: `Share ${selectedFile}`,
        UTI: 'public.json',
      });
    } catch (error) {
      Alert.alert('Error', 'Failed to share file.');
    }
  };

  const deleteFile = async () => {
    if (!selectedFile) return;
    const targetFile = selectedFile;
    closeMenu();

    Alert.alert('Delete', `Are you sure you want to delete ${formatDisplayName(targetFile)}?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          await FileSystem.deleteAsync(HISTORY_DIR + targetFile);
          loadFiles();
        },
      },
    ]);
  };

  const formatDisplayName = (fileName: string) => {
    return fileName.replace('.json', '').replace(/_\d+$/, '');
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000" />

      {/* Header */}
      <View style={styles.headerRow}>
        <Text style={styles.headerTitle}>Patient Histories</Text>
        <View style={{ flexDirection: "row", alignItems: "center", gap: 10 }}>
          <TouchableOpacity style={[styles.addButton, { backgroundColor: theme.card, borderWidth: 1, borderColor: theme.border }]} activeOpacity={0.8} onPress={() => router.push('./checklist')}>
            <Ionicons name="list" size={24} color="#fff" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.addButton} activeOpacity={0.8} onPress={() => router.push('./input')}>
            <Ionicons name="add" size={24} color="#fff" />
          </TouchableOpacity>
        </View>
      </View>

      {/* List */}
      <FlatList
        data={files}
        keyExtractor={(item) => item.name}
        contentContainerStyle={styles.listContainer}
        renderItem={({ item }) => {
          let actionBtnRef: View | null = null;
          return (
            <TouchableOpacity
              style={styles.card}
              activeOpacity={0.7}
              onPress={() => router.push(`./view?file=${encodeURIComponent(item.name)}`)}
            >
              <View style={styles.cardLeft}>
                <View style={styles.iconContainer}>
                  <Ionicons name="document-text-outline" size={22} color="#8e8e93" />
                </View>
                <View style={styles.textContainer}>
                  <Text style={styles.fileName} numberOfLines={1}>
                    {formatDisplayName(item.name)}
                  </Text>
                  <Text style={styles.fileSubtext}>{item.formattedDate || 'JSON Document'}</Text>
                </View>
              </View>

              <View style={styles.cardActions}>
                <TouchableOpacity
                  ref={(ref: any) => (actionBtnRef = ref)}
                  style={styles.actionIconBtn}
                  onPress={() => openMenu(item.name, actionBtnRef)}
                  hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                >
                  <Ionicons name="ellipsis-vertical" size={20} color="#8e8e93" />
                </TouchableOpacity>
              </View>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Ionicons name="folder-open-outline" size={48} color="#3a3a3c" />
            <Text style={styles.emptyText}>No histories found.</Text>
          </View>
        }
      />

      {/* Dropdown Menu Modal positioned directly below button */}
      <Modal visible={menuVisible} transparent animationType="fade" onRequestClose={closeMenu}>
        <TouchableWithoutFeedback onPress={closeMenu}>
          <View style={styles.modalOverlay}>
            <TouchableWithoutFeedback>
              <View style={[styles.menuContainer, { top: menuPosition.top, right: menuPosition.right }]}>
                <TouchableOpacity style={styles.menuItem} onPress={shareFile}>
                  <Ionicons name="share-outline" size={18} color="#007aff" style={styles.menuIcon} />
                  <Text style={styles.menuItemText}>Share File</Text>
                </TouchableOpacity>

                <View style={styles.menuDivider} />

                <TouchableOpacity style={styles.menuItem} onPress={deleteFile}>
                  <Ionicons name="trash-outline" size={18} color="#ff453a" style={styles.menuIcon} />
                  <Text style={[styles.menuItemText, styles.deleteText]}>Delete File</Text>
                </TouchableOpacity>
              </View>
            </TouchableWithoutFeedback>
          </View>
        </TouchableWithoutFeedback>
      </Modal>

      <Footer />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 60,
    paddingBottom: 20,
    backgroundColor: '#000000',
  },
  headerTitle: {
    fontSize: 28,
    fontWeight: '700',
    color: '#ffffff',
    letterSpacing: -0.5,
  },
  addButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#007aff',
    justifyContent: 'center',
    alignItems: 'center',
  },
  listContainer: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 20,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#1c1c1e',
    padding: 16,
    borderRadius: 16,
    marginBottom: 12,
  },
  cardLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    marginRight: 12,
  },
  iconContainer: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#2c2c2e',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  textContainer: {
    flex: 1,
  },
  fileName: {
    fontSize: 17,
    fontWeight: '600',
    color: '#ffffff',
    marginBottom: 2,
  },
  fileSubtext: {
    fontSize: 13,
    color: '#8e8e93',
  },
  cardActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  actionIconBtn: {
    padding: 4,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 100,
  },
  emptyText: {
    marginTop: 12,
    fontSize: 16,
    color: '#8e8e93',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.3)',
  },
  menuContainer: {
    position: 'absolute',
    width: 180,
    backgroundColor: '#2c2c2e',
    borderRadius: 14,
    paddingVertical: 8,
    paddingHorizontal: 12,
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
  menuHeaderTitle: {
    fontSize: 12,
    fontWeight: '600',
    color: '#8e8e93',
    marginVertical: 4,
    textAlign: 'left',
    paddingHorizontal: 4,
    textTransform: 'uppercase',
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 4,
  },
  menuIcon: {
    marginRight: 10,
  },
  menuItemText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#ffffff',
  },
  deleteText: {
    color: '#ff453a',
  },
  menuDivider: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: '#3a3a3c',
    marginVertical: 4,
  },
});