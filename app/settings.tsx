import { FontAwesome5, Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Clipboard from 'expo-clipboard';
import { Directory, File, Paths } from 'expo-file-system';
import * as FileSystemLegacy from 'expo-file-system/legacy';
import { useRouter } from 'expo-router';
import * as SecureStore from 'expo-secure-store';
import React, { useEffect, useState } from 'react';
import {
  Alert,
  LayoutAnimation,
  Platform,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  UIManager,
  View,
} from 'react-native';
import Footer from './footer';
import defaultTemplate from './history/template.json';

// Enable LayoutAnimation on Android
if (Platform.OS === 'android' && UIManager.setLayoutAnimationEnabledExperimental) {
  UIManager.setLayoutAnimationEnabledExperimental(true);
}

const TEMPLATE_FILE_URI = FileSystemLegacy.documentDirectory + 'template.json';
const NOTES_KEY = 'notesKey';
const CATEGORIES_KEY = 'categoriesKey';
const DEFAULT_CATEGORIES = ['Work', 'Personal', 'Ideas', 'To-Do'];


export default function Settings() {
  const router = useRouter();
  const [apiKey, setApiKey] = useState('');
  const [model, setModel] = useState('gemini-2.5-flash');
  const [templateStr, setTemplateStr] = useState('');

  // UI Collapsible states
  const [showKey, setShowKey] = useState(false);
  const [isAiCollapsed, setIsAiCollapsed] = useState(true);
  const [isBackupCollapsed, setIsBackupCollapsed] = useState(true);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    const key = await SecureStore.getItemAsync('GEMINI_API_KEY');
    const storedModel = await SecureStore.getItemAsync('GEMINI_MODEL');
    if (key) setApiKey(key);
    if (storedModel) setModel(storedModel);

    const fileInfo = await FileSystemLegacy.getInfoAsync(TEMPLATE_FILE_URI);
    if (fileInfo.exists) {
      const storedTemplate = await FileSystemLegacy.readAsStringAsync(TEMPLATE_FILE_URI);
      setTemplateStr(storedTemplate);
    } else {
      setTemplateStr(JSON.stringify(defaultTemplate, null, 2));
    }
  };

  const saveSettings = async () => {
    try {
      await SecureStore.setItemAsync('GEMINI_API_KEY', apiKey);
      await SecureStore.setItemAsync('GEMINI_MODEL', model);

      JSON.parse(templateStr);
      await FileSystemLegacy.writeAsStringAsync(TEMPLATE_FILE_URI, templateStr);

      Alert.alert('Success', 'Settings saved successfully!');
    } catch (e) {
      Alert.alert('Error', 'Invalid JSON Template or error saving settings.');
    }
  };

  const toggleAiCollapse = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setIsAiCollapsed(!isAiCollapsed);
  };

  const toggleBackupCollapse = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setIsBackupCollapsed(!isBackupCollapsed);
  };

  const copyToClipboard = async () => {
    if (!apiKey) {
      Alert.alert('Notice', 'No API key to copy.');
      return;
    }
    await Clipboard.setStringAsync(apiKey);
    Alert.alert('Copied', 'API Key copied to clipboard.');
  };

  // Optional app state refresh hooks post-restore
  const loadData = async () => { };
  const loadEntries = async () => { };

  // Helper function to detect image extensions
  const getMimeType = (filename: any) => {
    const ext = filename.split('.').pop().toLowerCase();
    switch (ext) {
      case 'png':
        return 'image/png';
      case 'jpg':
      case 'jpeg':
        return 'image/jpeg';
      case 'gif':
        return 'image/gif';
      case 'webp':
        return 'image/webp';
      default:
        return 'application/json';
    }
  };

  // --- Backup & Restore Handlers ---

  const handleBackupData = async () => {
    try {
      const currentNotes = await AsyncStorage.getItem(NOTES_KEY);
      const currentCategories = await AsyncStorage.getItem(CATEGORIES_KEY);
      const currentTransactions = await AsyncStorage.getItem('transactions');
      const currentTodo = await AsyncStorage.getItem('dbKey');
      const currentPasswords = await AsyncStorage.getItem('user_passwords_list');

      const appDir = new Directory(Paths.document, 'App');
      const historyDir = new Directory(Paths.document, 'histories');

      const hasAsyncData =
        currentNotes ||
        currentCategories ||
        currentTransactions ||
        currentTodo ||
        currentPasswords;
      const hasDiaryData = appDir.exists && appDir.list().length > 0;
      const hasHistoryData = historyDir.exists && historyDir.list().length > 0;

      if (!hasAsyncData && !hasDiaryData && !hasHistoryData) {
        Alert.alert('Notice', 'No data found to backup.');
        return;
      }

      const backupDir = await Directory.pickDirectoryAsync();
      if (!backupDir) return;

      for (const item of backupDir.list()) {
        await item.delete();
      }

      // 1. Backup AsyncStorage to /Notes
      const notesBackupDir = backupDir.createDirectory('AppData');

      const notesFile = notesBackupDir.createFile('notes.json', 'application/json');
      await notesFile.write(currentNotes || '[]');

      const categoriesFile = notesBackupDir.createFile('categories.json', 'application/json');
      await categoriesFile.write(currentCategories || JSON.stringify(DEFAULT_CATEGORIES));

      const transactionsFile = notesBackupDir.createFile('transactions.json', 'application/json');
      await transactionsFile.write(currentTransactions || '[]');

      const todoFile = notesBackupDir.createFile('todo.json', 'application/json');
      await todoFile.write(currentTodo || '[]');

      const passwordsFile = notesBackupDir.createFile('passwords.json', 'application/json');
      await passwordsFile.write(currentPasswords || '[]');

      // Recursive copy helper with MIME-type resolution
      const backupFolderContents = async (sourceDir: any, targetBackupDir: any) => {
        for (const item of sourceDir.list()) {
          if (item instanceof File) {
            const mimeType = getMimeType(item.name);
            const newFile = targetBackupDir.createFile(item.name, mimeType);
            const data = await item.bytes();
            await newFile.write(data);
          } else if (item instanceof Directory) {
            const newSubDir = targetBackupDir.createDirectory(item.name);
            await backupFolderContents(item, newSubDir);
          }
        }
      };

      // 2. Backup App Directory to /Diary
      if (appDir.exists) {
        const diaryBackupDir = backupDir.createDirectory('Diary');
        await backupFolderContents(appDir, diaryBackupDir);
      }

      // 3. Backup Histories Directory to /History
      if (historyDir.exists) {
        const historyBackupDir = backupDir.createDirectory('History');
        await backupFolderContents(historyDir, historyBackupDir);
      }

      Alert.alert('Success', 'Backup completed successfully!');
    } catch (err) {
      console.error('Backup error:', err);
      Alert.alert('Error', 'Failed to complete backup.');
    }
  };

  const restoreData = async () => {
    try {
      const backupDir = await Directory.pickDirectoryAsync();
      if (!backupDir) return;

      let notesRestored = false;
      let diaryRestored = false;
      let historyRestored = false;

      const appDir = new Directory(Paths.document, 'App');
      if (appDir.exists) {
        for (const item of appDir.list()) {
          await item.delete();
        }
      } else {
        appDir.create();
      }

      const historyDir = new Directory(Paths.document, 'histories');
      if (historyDir.exists) {
        for (const item of historyDir.list()) {
          await item.delete();
        }
      } else {
        historyDir.create();
      }

      const restoreFolderContents = async (sourceBackupDir: any, localTargetDir: any) => {
        for (const item of sourceBackupDir.list()) {
          if (item instanceof File) {
            const newFile = new File(localTargetDir, item.name);
            newFile.create();
            const data = await item.bytes();
            await newFile.write(data);
          } else if (item instanceof Directory) {
            const newSubDir = new Directory(localTargetDir, item.name);
            if (!newSubDir.exists) newSubDir.create();
            await restoreFolderContents(item, newSubDir);
          }
        }
      };

      for (const topItem of backupDir.list()) {
        if (!(topItem instanceof Directory)) continue;

        if (topItem.name === 'AppData') {
          for (const file of topItem.list()) {
            if (file instanceof File) {
              const text = await file.text();
              if (file.name === 'notes.json') {
                await AsyncStorage.setItem(NOTES_KEY, text);
              } else if (file.name === 'categories.json') {
                await AsyncStorage.setItem(CATEGORIES_KEY, text);
              } else if (file.name === 'transactions.json') {
                await AsyncStorage.setItem('transactions', text);
              } else if (file.name === 'todo.json') {
                await AsyncStorage.setItem('dbKey', text);
              } else if (file.name === 'passwords.json') {
                await AsyncStorage.setItem('user_passwords_list', text);
              }
            }
          }
          notesRestored = true;
        } else if (topItem.name === 'Diary') {
          await restoreFolderContents(topItem, appDir);
          diaryRestored = true;
        } else if (topItem.name === 'History') {
          await restoreFolderContents(topItem, historyDir);
          historyRestored = true;
        }
      }

      if (notesRestored) await loadData();
      if (diaryRestored || historyRestored) await loadEntries();

      Alert.alert('Success', 'Data restored successfully!');
    } catch (err) {
      console.error('Restore error:', err);
      Alert.alert('Error', 'Failed to restore data.');
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000000" />

      {/* Header */}
      <View style={styles.headerRow}>
        <Text style={styles.headerTitle}>Settings</Text>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
        {/* Collapsible Section Header: AI Config */}
        <TouchableOpacity
          style={styles.sectionHeader}
          activeOpacity={0.7}
          onPress={toggleAiCollapse}
        >
          <View style={styles.sectionHeaderTitleGroup}>
            <Ionicons name="sparkles-outline" size={20} color="#007aff" style={{ marginRight: 10 }} />
            <Text style={styles.sectionHeaderTitle}>AI Configuration</Text>
          </View>
          <Ionicons
            name={isAiCollapsed ? 'chevron-forward' : 'chevron-down'}
            size={20}
            color="#8e8e93"
          />
        </TouchableOpacity>

        {/* Collapsible Content Card: AI Config */}
        {!isAiCollapsed && (
          <View style={styles.collapsibleContent}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>Gemini API Key</Text>
              <View style={styles.actionRow}>
                <TouchableOpacity onPress={() => setShowKey(!showKey)} style={styles.iconBtn}>
                  <Ionicons name={showKey ? 'eye-off-outline' : 'eye-outline'} size={18} color="#8e8e93" />
                </TouchableOpacity>
                <TouchableOpacity onPress={copyToClipboard} style={styles.iconBtn}>
                  <Ionicons name="copy-outline" size={18} color="#8e8e93" />
                </TouchableOpacity>
              </View>
            </View>

            <View style={styles.inputCard}>
              <TextInput
                style={styles.input}
                value={apiKey}
                onChangeText={setApiKey}
                secureTextEntry={!showKey}
                placeholder="Enter API Key"
                placeholderTextColor="#636366"
                autoCapitalize="none"
              />
            </View>

            <Text style={styles.label}>Model Selection</Text>

            <View style={styles.segmentedControl}>
              <TouchableOpacity
                style={[
                  styles.segmentBtn,
                  model === 'gemini-2.5-flash' && styles.segmentBtnActive,
                ]}
                onPress={() => setModel('gemini-2.5-flash')}
                activeOpacity={0.8}
              >
                <Text
                  style={[
                    styles.segmentText,
                    model === 'gemini-2.5-flash' && styles.segmentTextActive,
                  ]}
                >
                  2.5 Flash
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[
                  styles.segmentBtn,
                  model === 'gemini-3.5-flash' && styles.segmentBtnActive,
                ]}
                onPress={() => setModel('gemini-3.5-flash')}
                activeOpacity={0.8}
              >
                <Text
                  style={[
                    styles.segmentText,
                    model === 'gemini-3.5-flash' && styles.segmentTextActive,
                  ]}
                >
                  3.5 Flash
                </Text>
              </TouchableOpacity>
            </View>

            <Text style={styles.note}>Active Model: {model}</Text>

            <TouchableOpacity style={styles.saveButton} onPress={saveSettings} activeOpacity={0.8}>
              <Text style={styles.saveButtonText}>Save Settings</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Collapsible Section Header: Backup & Restore */}
        <TouchableOpacity
          style={styles.sectionHeader}
          activeOpacity={0.7}
          onPress={toggleBackupCollapse}
        >
          <View style={styles.sectionHeaderTitleGroup}>
            <Ionicons name="cloud-upload-outline" size={20} color="#007aff" style={{ marginRight: 10 }} />
            <Text style={styles.sectionHeaderTitle}>Backup & Restore</Text>
          </View>
          <Ionicons
            name={isBackupCollapsed ? 'chevron-forward' : 'chevron-down'}
            size={20}
            color="#8e8e93"
          />
        </TouchableOpacity>

        {/* Collapsible Content Card: Backup & Restore */}
        {!isBackupCollapsed && (
          <View style={styles.collapsibleContent}>
            <Text style={styles.backupDescription}>
              Export all app notes, passwords, transactions, and local files into a directory or restore them from an existing backup.
            </Text>

            <TouchableOpacity style={styles.actionButton} onPress={handleBackupData} activeOpacity={0.8}>
              <Ionicons name="cloud-upload-outline" size={18} color="#ffffff" style={{ marginRight: 8 }} />
              <Text style={styles.actionButtonText}>Backup Data</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.actionButton, styles.restoreButton]}
              onPress={restoreData}
              activeOpacity={0.8}
            >
              <Ionicons name="cloud-download-outline" size={18} color="#ffffff" style={{ marginRight: 8 }} />
              <Text style={styles.actionButtonText}>Restore Data</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* Password Tracker Navigation Card */}
        <TouchableOpacity
          style={styles.navigationCard}
          activeOpacity={0.7}
          onPress={() => router.push('/password_tracker/password')}
        >
          <View style={styles.navigationCardTitleGroup}>
            <Ionicons name="key-outline" size={20} color="#007aff" style={{ marginRight: 10 }} />
            <Text style={styles.navigationCardTitle}>Password Tracker</Text>
          </View>
          <Ionicons name="chevron-forward" size={20} color="#8e8e93" />
        </TouchableOpacity>

        {/* History Navigation Card */}
        <TouchableOpacity
          style={styles.navigationCard}
          activeOpacity={0.7}
          onPress={() => router.push('/history/history')}
        >
          <View style={styles.navigationCardTitleGroup}>
            <FontAwesome5 name="user-md" size={20} color="#007aff" style={{ marginRight: 10 }} />
            <Text style={styles.navigationCardTitle}>Clinical Tracker</Text>
          </View>
          <Ionicons name="chevron-forward" size={20} color="#8e8e93" />
        </TouchableOpacity>
      </ScrollView>

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
  scrollContainer: {
    paddingHorizontal: 16,
    paddingBottom: 20,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#1c1c1e',
    padding: 16,
    borderRadius: 16,
    marginBottom: 8,
  },
  sectionHeaderTitleGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  sectionHeaderTitle: {
    fontSize: 17,
    fontWeight: '600',
    color: '#ffffff',
  },
  collapsibleContent: {
    backgroundColor: '#1c1c1e',
    padding: 16,
    borderRadius: 16,
    marginBottom: 16,
    marginTop: 4,
  },
  navigationCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#1c1c1e',
    padding: 16,
    borderRadius: 16,
    marginBottom: 8,
  },
  navigationCardTitleGroup: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  navigationCardTitle: {
    fontSize: 17,
    fontWeight: '600',
    color: '#ffffff',
  },
  labelRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: '#8e8e93',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginTop: 10,
    marginBottom: 8,
  },
  actionRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  iconBtn: {
    marginLeft: 12,
    padding: 4,
  },
  inputCard: {
    backgroundColor: '#2c2c2e',
    borderRadius: 12,
    paddingHorizontal: 12,
    height: 44,
    justifyContent: 'center',
    marginBottom: 12,
  },
  input: {
    color: '#ffffff',
    fontSize: 16,
  },
  segmentedControl: {
    flexDirection: 'row',
    backgroundColor: '#2c2c2e',
    borderRadius: 10,
    padding: 3,
    marginTop: 4,
  },
  segmentBtn: {
    flex: 1,
    paddingVertical: 8,
    alignItems: 'center',
    borderRadius: 8,
  },
  segmentBtnActive: {
    backgroundColor: '#3a3a3c',
  },
  segmentText: {
    color: '#8e8e93',
    fontWeight: '500',
    fontSize: 14,
  },
  segmentTextActive: {
    color: '#ffffff',
    fontWeight: '600',
  },
  note: {
    fontSize: 12,
    color: '#636366',
    marginTop: 8,
  },
  saveButton: {
    backgroundColor: '#007aff',
    height: 50,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 20,
  },
  saveButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
  backupDescription: {
    fontSize: 14,
    color: '#8e8e93',
    marginBottom: 16,
    lineHeight: 20,
  },
  actionButton: {
    backgroundColor: '#007aff',
    height: 48,
    borderRadius: 12,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 10,
  },
  restoreButton: {
    backgroundColor: '#34c759',
    marginBottom: 0,
  },
  actionButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
});