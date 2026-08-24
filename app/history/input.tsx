import { Ionicons } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system/legacy';
import * as ImagePicker from 'expo-image-picker';
import { useRouter } from 'expo-router';
import * as SecureStore from 'expo-secure-store';
import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Image,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import Footer from '../footer';
import defaultTemplate from './template.json';

const HISTORY_DIR = FileSystem.documentDirectory + 'histories/';
const TEMPLATE_FILE_URI = FileSystem.documentDirectory + 'template.json';

interface ImageFile {
  uri: string;
  base64: string;
}

export default function InputScreen() {
  const [selectedImages, setSelectedImages] = useState<ImageFile[]>([]);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const pickImages = async () => {
    let result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsMultipleSelection: true,
      base64: true,
      quality: 0.8,
    });

    if (!result.canceled && result.assets && result.assets.length > 0) {
      const newImages: ImageFile[] = result.assets
        .filter((asset) => asset.base64)
        .map((asset) => ({
          uri: asset.uri,
          base64: asset.base64 as string,
        }));

      setSelectedImages((prev) => [...prev, ...newImages]);
    }
  };

  const removeImage = (index: number) => {
    setSelectedImages((prev) => prev.filter((_, i) => i !== index));
  };

  const clearImages = () => {
    setSelectedImages([]);
  };

  const processImages = async () => {
    if (selectedImages.length === 0) {
      Alert.alert("Error", "Please select at least one image first.");
      return;
    }

    setLoading(true);
    try {
      const apiKey = await SecureStore.getItemAsync('GEMINI_API_KEY');
      const model = (await SecureStore.getItemAsync('GEMINI_MODEL')) || 'gemini-2.5-flash';

      if (!apiKey) {
        Alert.alert("Error", "Please configure Gemini API Key in Settings.");
        setLoading(false);
        return;
      }

      // Load Template
      let templateStr = JSON.stringify(defaultTemplate);
      const fileInfo = await FileSystem.getInfoAsync(TEMPLATE_FILE_URI);
      if (fileInfo.exists) {
        templateStr = await FileSystem.readAsStringAsync(TEMPLATE_FILE_URI);
      }

      const prompt = `You are a medical assistant extracting clinical history from handwritten notes across ${selectedImages.length} page(s). 
      Analyze all provided images in sequence and fill in the following JSON template with the consolidated extracted information. 
      Return strictly valid JSON without any markdown code blocks. 
      Template: ${templateStr}`;

      // Build payload parts containing prompt + all selected image parts
      const parts: any[] = [{ text: prompt }];

      selectedImages.forEach((img) => {
        parts.push({
          inline_data: {
            mime_type: "image/jpeg",
            data: img.base64,
          },
        });
      });

      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            contents: [{ parts }],
          }),
        }
      );

      const data = await response.json();

      if (data.error) throw new Error(data.error.message);

      let textOutput = data.candidates[0].content.parts[0].text;

      // Strip markdown backticks if Gemini accidentally adds them
      textOutput = textOutput.replace(/```json/g, '').replace(/```/g, '').trim();

      const parsedData = JSON.parse(textOutput); // Validate JSON

      // Save to File System
      const dirInfo = await FileSystem.getInfoAsync(HISTORY_DIR);
      if (!dirInfo.exists) {
        await FileSystem.makeDirectoryAsync(HISTORY_DIR, { intermediates: true });
      }

      const patientName =
        parsedData?.clinical_history_template?.['1_patient_identification']?.name || 'Unknown';
      const fileName = `${patientName.replace(/[^a-zA-Z0-9]/g, '_')}_${Date.now()}.json`;
      const filePath = HISTORY_DIR + fileName;

      await FileSystem.writeAsStringAsync(filePath, JSON.stringify(parsedData, null, 2));

      Alert.alert("Success", "History extracted and saved successfully!");
      router.push(`./history`);
    } catch (error: any) {
      Alert.alert("Processing Error", error.message || "Failed to extract data.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#000000" />

      {/* Header */}
      <View style={styles.headerRow}>
        <Text style={styles.headerTitle}>Upload Note</Text>
        {selectedImages.length > 0 && (
          <TouchableOpacity onPress={clearImages}>
            <Text style={styles.clearText}>Clear All</Text>
          </TouchableOpacity>
        )}
      </View>

      <ScrollView contentContainerStyle={styles.scrollContainer}>
        {/* Gallery / Placeholder Viewport */}
        {selectedImages.length > 0 ? (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.galleryContainer}
          >
            {selectedImages.map((img, index) => (
              <View key={index} style={styles.imageCard}>
                <Image source={{ uri: img.uri }} style={styles.imagePreview} />
                <TouchableOpacity
                  style={styles.removeBadge}
                  onPress={() => removeImage(index)}
                  hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
                >
                  <Ionicons name="close-circle" size={24} color="#ff453a" />
                </TouchableOpacity>
                <View style={styles.pageBadge}>
                  <Text style={styles.pageBadgeText}>Page {index + 1}</Text>
                </View>
              </View>
            ))}
          </ScrollView>
        ) : (
          <TouchableOpacity
            style={styles.placeholderCard}
            activeOpacity={0.8}
            onPress={pickImages}
          >
            <View style={styles.iconCircle}>
              <Ionicons name="documents-outline" size={32} color="#007aff" />
            </View>
            <Text style={styles.placeholderText}>Tap to select handwritten history page(s)</Text>
            <Text style={styles.placeholderSubtext}>Supports multiple JPEG or PNG images</Text>
          </TouchableOpacity>
        )}

        {/* Action Controls */}
        <View style={styles.actionGroup}>
          <TouchableOpacity
            style={styles.secondaryBtn}
            activeOpacity={0.8}
            onPress={pickImages}
          >
            <Ionicons name="images-outline" size={20} color="#007aff" style={{ marginRight: 8 }} />
            <Text style={styles.secondaryBtnText}>
              {selectedImages.length > 0 ? 'Add More Images' : 'Pick Images'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.primaryBtn,
              (selectedImages.length === 0 || loading) && styles.disabledBtn,
            ]}
            activeOpacity={0.8}
            onPress={processImages}
            disabled={selectedImages.length === 0 || loading}
          >
            {loading ? (
              <ActivityIndicator color="#ffffff" size="small" />
            ) : (
              <>
                <Ionicons name="sparkles-outline" size={20} color="#ffffff" style={{ marginRight: 8 }} />
                <Text style={styles.primaryBtnText}>
                  Extract & Save ({selectedImages.length} {selectedImages.length === 1 ? 'Page' : 'Pages'})
                </Text>
              </>
            )}
          </TouchableOpacity>
        </View>
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
  clearText: {
    color: '#ff453a',
    fontSize: 16,
    fontWeight: '500',
  },
  scrollContainer: {
    paddingHorizontal: 16,
    paddingBottom: 20,
  },
  galleryContainer: {
    paddingVertical: 8,
    marginBottom: 20,
  },
  imageCard: {
    width: 260,
    height: 380,
    backgroundColor: '#1c1c1e',
    borderRadius: 20,
    overflow: 'hidden',
    marginRight: 12,
    position: 'relative',
  },
  imagePreview: {
    width: '100%',
    height: '100%',
    resizeMode: 'cover',
  },
  removeBadge: {
    position: 'absolute',
    top: 10,
    right: 10,
    backgroundColor: '#00000088',
    borderRadius: 12,
  },
  pageBadge: {
    position: 'absolute',
    bottom: 12,
    left: 12,
    backgroundColor: 'rgba(0,0,0,0.7)',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 8,
  },
  pageBadgeText: {
    color: '#ffffff',
    fontSize: 12,
    fontWeight: '600',
  },
  placeholderCard: {
    width: '100%',
    height: 380,
    backgroundColor: '#1c1c1e',
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
    paddingHorizontal: 20,
  },
  iconCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: '#2c2c2e',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  placeholderText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
    textAlign: 'center',
    marginBottom: 6,
  },
  placeholderSubtext: {
    color: '#8e8e93',
    fontSize: 13,
    textAlign: 'center',
  },
  actionGroup: {
    width: '100%',
  },
  secondaryBtn: {
    flexDirection: 'row',
    backgroundColor: '#1c1c1e',
    height: 50,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  secondaryBtnText: {
    color: '#007aff',
    fontSize: 16,
    fontWeight: '600',
  },
  primaryBtn: {
    flexDirection: 'row',
    backgroundColor: '#007aff',
    height: 50,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
  },
  disabledBtn: {
    backgroundColor: '#2c2c2e',
    opacity: 0.6,
  },
  primaryBtnText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '600',
  },
});