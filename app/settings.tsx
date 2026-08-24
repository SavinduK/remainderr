import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import * as FileSystem from 'expo-file-system/legacy';
import { useRouter } from 'expo-router'; // Added router import
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

const TEMPLATE_FILE_URI = FileSystem.documentDirectory + 'template.json';

export default function Settings() {
  const router = useRouter(); // Initialize router
  const [apiKey, setApiKey] = useState('');
  const [model, setModel] = useState('gemini-2.5-flash');
  const [templateStr, setTemplateStr] = useState('');

  // New UI states
  const [showKey, setShowKey] = useState(false);
  const [isAiCollapsed, setIsAiCollapsed] = useState(false);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    const key = await SecureStore.getItemAsync('GEMINI_API_KEY');
    const storedModel = await SecureStore.getItemAsync('GEMINI_MODEL');
    if (key) setApiKey(key);
    if (storedModel) setModel(storedModel);

    const fileInfo = await FileSystem.getInfoAsync(TEMPLATE_FILE_URI);
    if (fileInfo.exists) {
      const storedTemplate = await FileSystem.readAsStringAsync(TEMPLATE_FILE_URI);
      setTemplateStr(storedTemplate);
    } else {
      setTemplateStr(JSON.stringify(defaultTemplate, null, 2));
    }
  };

  const saveSettings = async () => {
    try {
      await SecureStore.setItemAsync('GEMINI_API_KEY', apiKey);
      await SecureStore.setItemAsync('GEMINI_MODEL', model);

      // Validate JSON before saving
      JSON.parse(templateStr);
      await FileSystem.writeAsStringAsync(TEMPLATE_FILE_URI, templateStr);

      Alert.alert('Success', 'Settings saved successfully!');
    } catch (e) {
      Alert.alert('Error', 'Invalid JSON Template or error saving settings.');
    }
  };

  const toggleCollapse = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setIsAiCollapsed(!isAiCollapsed);
  };

  const copyToClipboard = async () => {
    if (!apiKey) {
      Alert.alert('Notice', 'No API key to copy.');
      return;
    }
    await Clipboard.setStringAsync(apiKey);
    Alert.alert('Copied', 'API Key copied to clipboard.');
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000000" />

      {/* Header */}
      <View style={styles.headerRow}>
        <Text style={styles.headerTitle}>Settings</Text>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
        {/* Collapsible Section Header */}
        <TouchableOpacity
          style={styles.sectionHeader}
          activeOpacity={0.7}
          onPress={toggleCollapse}
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

        {/* Collapsible Content Card */}
        {!isAiCollapsed && (
          <View style={styles.collapsibleContent}>
            {/* Input Label & Actions */}
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

            {/* API Key Input Container */}
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

            {/* Model Selection Label */}
            <Text style={styles.label}>Model Selection</Text>

            {/* Custom Model Selector Chips */}
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

            {/* Save Action Button Inside Card */}
            <TouchableOpacity style={styles.saveButton} onPress={saveSettings} activeOpacity={0.8}>
              <Text style={styles.saveButtonText}>Save Settings</Text>
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
            <Ionicons name="key-outline" size={20} color="#007aff" style={{ marginRight: 10 }} />
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
});