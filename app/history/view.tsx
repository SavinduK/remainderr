import { Ionicons } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import { useLocalSearchParams, useRouter } from 'expo-router';
import React, { useEffect, useState } from 'react';
import {
  Alert,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import Footer from '../footer';

const HISTORY_DIR = FileSystem.documentDirectory + 'histories/';

export default function ViewHistory() {
  const { file } = useLocalSearchParams();
  const router = useRouter();
  const [jsonData, setJsonData] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    if (file) {
      loadFile(file as string);
    }
  }, [file]);

  const loadFile = async (fileName: string) => {
    try {
      const content = await FileSystem.readAsStringAsync(HISTORY_DIR + fileName);
      setJsonData(content);
    } catch (e) {
      Alert.alert('Error', 'Could not load file.');
      router.back();
    }
  };

  const saveFile = async () => {
    try {
      JSON.parse(jsonData); // Validate JSON format
      await FileSystem.writeAsStringAsync(HISTORY_DIR + (file as string), jsonData);
      setIsEditing(false);
      Alert.alert('Saved', 'Patient history updated successfully.');
    } catch (e) {
      Alert.alert('Invalid Format', 'Ensure the text is valid JSON before saving.');
    }
  };

  const formatKeyName = (key: string) => {
    return key
      .replace(/^[\d_]+/, '')
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (char) => char.toUpperCase());
  };

  const renderFormattedData = (data: any, depth = 0): React.ReactNode => {
    if (data === null || data === undefined) return null;

    if (typeof data !== 'object') {
      return <Text style={styles.valueText}>{String(data)}</Text>;
    }

    if (Array.isArray(data)) {
      if (data.length === 0) return null;
      return (
        <View style={{ paddingLeft: depth * 10 }}>
          {data.map((item, index) => {
            if (item === null || item === undefined) return null;
            return (
              <View key={index} style={styles.bulletRow}>
                <Ionicons name="ellipse" size={6} color="#007aff" style={styles.bulletSymbol} />
                <View style={styles.bulletContent}>
                  {typeof item === 'object' ? (
                    renderFormattedData(item, depth + 1)
                  ) : (
                    <Text style={styles.bulletText}>{String(item)}</Text>
                  )}
                </View>
              </View>
            );
          })}
        </View>
      );
    }

    const keys = Object.keys(data).filter((key) => data[key] !== null && data[key] !== undefined);
    if (keys.length === 0) return null;

    return (
      <View style={{ paddingLeft: depth > 0 ? 10 : 0 }}>
        {keys.map((key) => {
          const val = data[key];
          if (Array.isArray(val) && val.length === 0) return null;
          if (
            typeof val === 'object' &&
            val !== null &&
            !Array.isArray(val) &&
            Object.keys(val).length === 0
          )
            return null;

          const isTopLevel = depth === 0;

          return (
            <View
              key={key}
              style={[
                styles.sectionContainer,
                isTopLevel && styles.topLevelSection,
              ]}
            >
              <Text style={[styles.keyHeader, isTopLevel && styles.topLevelKeyHeader]}>
                {formatKeyName(key)}
              </Text>
              {renderFormattedData(val, depth + 1)}
            </View>
          );
        })}
      </View>
    );
  };

  const renderViewContent = () => {
    try {
      const parsed = JSON.parse(jsonData);
      return renderFormattedData(parsed);
    } catch (e) {
      return <Text style={styles.viewer}>{jsonData}</Text>;
    }
  };

  if (!file)
    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyText}>No file selected</Text>
      </View>
    );

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000000" />

      {/* Header */}
      <View style={styles.headerRow}>
        <TouchableOpacity style={styles.backBtn} onPress={() => router.back()}>
          <Ionicons name="chevron-back" size={24} color="#007aff" />
        </TouchableOpacity>
        <Text style={styles.headerTitle} numberOfLines={1}>
          {(file as string).replace('.json', '')}
        </Text>
        <TouchableOpacity
          style={styles.editBtn}
          onPress={() => {
            if (isEditing) loadFile(file as string);
            setIsEditing(!isEditing);
          }}
        >
          <Text style={styles.editBtnText}>{isEditing ? 'Cancel' : 'Edit'}</Text>
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
        {isEditing ? (
          <View style={styles.editorContainer}>
            <TextInput
              style={styles.editor}
              value={jsonData}
              onChangeText={setJsonData}
              multiline
              placeholderTextColor="#636366"
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>
        ) : (
          <View style={styles.formattedCard}>{renderViewContent()}</View>
        )}

        {isEditing && (
          <TouchableOpacity style={styles.saveBtn} onPress={saveFile} activeOpacity={0.8}>
            <Ionicons name="checkmark" size={20} color="#ffffff" style={{ marginRight: 6 }} />
            <Text style={styles.saveBtnText}>Save Changes</Text>
          </TouchableOpacity>
        )}
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
  emptyContainer: {
    flex: 1,
    backgroundColor: '#000000',
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    color: '#8e8e93',
    fontSize: 16,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingTop: 56,
    paddingBottom: 16,
    backgroundColor: '#000000',
    borderBottomWidth: 1,
    borderBottomColor: '#1c1c1e',
  },
  backBtn: {
    padding: 4,
    marginRight: 8,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#ffffff',
    flex: 1,
  },
  editBtn: {
    backgroundColor: '#2c2c2e',
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 16,
    marginLeft: 12,
  },
  editBtnText: {
    color: '#007aff',
    fontSize: 14,
    fontWeight: '600',
  },
  scrollContainer: {
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 24,
  },
  formattedCard: {
    backgroundColor: '#1c1c1e',
    borderRadius: 16,
    padding: 16,
  },
  sectionContainer: {
    marginTop: 8,
    marginBottom: 4,
  },
  topLevelSection: {
    borderBottomWidth: 1,
    borderBottomColor: '#2c2c2e',
    paddingBottom: 12,
    marginBottom: 12,
  },
  keyHeader: {
    fontSize: 14,
    fontWeight: '600',
    color: '#8e8e93',
    marginBottom: 4,
  },
  topLevelKeyHeader: {
    fontSize: 16,
    fontWeight: '700',
    color: '#007aff',
    marginBottom: 6,
  },
  bulletRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginVertical: 3,
  },
  bulletSymbol: {
    marginRight: 8,
    marginTop: 6,
  },
  bulletContent: {
    flex: 1,
  },
  bulletText: {
    fontSize: 15,
    color: '#ffffff',
    lineHeight: 20,
  },
  valueText: {
    fontSize: 15,
    color: '#ebebf5',
    marginLeft: 2,
  },
  editorContainer: {
    backgroundColor: '#1c1c1e',
    borderRadius: 16,
    padding: 12,
    minHeight: 450,
  },
  editor: {
    color: '#ffffff',
    fontFamily: 'monospace',
    fontSize: 14,
    textAlignVertical: 'top',
    flex: 1,
  },
  viewer: {
    color: '#ffffff',
    fontFamily: 'monospace',
    fontSize: 14,
  },
  saveBtn: {
    flexDirection: 'row',
    backgroundColor: '#34c759',
    height: 50,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 16,
  },
  saveBtnText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
});