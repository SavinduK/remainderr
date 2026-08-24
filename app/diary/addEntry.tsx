import React, { useState } from "react";
import {
  Image,
  KeyboardAvoidingView,
  Modal,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useColorScheme,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { KeyboardAwareScrollView } from "react-native-keyboard-aware-scroll-view";
import { Ionicons } from "@expo/vector-icons";
import * as ImagePicker from "expo-image-picker";
import { Directory, File, Paths } from "expo-file-system";
import { useRouter } from "expo-router";
import DateTimePicker from "@react-native-community/datetimepicker";

export default function AddEntry() {
  const router = useRouter();
  const colorScheme = useColorScheme();
  const isDark = colorScheme === "dark";

  const [title, setTitle] = useState("");
  const [text, setText] = useState("");
  const [imageUris, setImageUris] = useState<string[]>([]);
  const [date, setDate] = useState(new Date());
  const [showDatePicker, setShowDatePicker] = useState(false);

  const themeColors = {
    bg: isDark ? "#000000" : "#FFFFFF",
    text: isDark ? "#FFFFFF" : "#000000",
    secondaryText: isDark ? "#A0A0A0" : "#666666",
    cardBg: isDark ? "#1C1C1E" : "#F2F2F7",
    accent: "#0A84FF",
    placeholder: isDark ? "#636366" : "#8E8E93",
  };

  const pickImages = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
      allowsMultipleSelection: true,
    });

    if (!result.canceled && result.assets) {
      const selectedUris = result.assets.map((asset) => asset.uri);
      setImageUris((prev) => [...prev, ...selectedUris]);
    }
  };

  const removeImage = (index: number) => {
    setImageUris((prev) => prev.filter((_, i) => i !== index));
  };

  const submit = async () => {
    if (imageUris.length === 0 && text.trim() === "" && title.trim() === "") return;

    try {
      // 1. Create unique timestamp string format: YYYY-MM-DD_HH-mm-ss
      const now = new Date();
      const timestampString = now.toISOString().replace(/[:.]/g, "-");
      
      const appDir = new Directory(Paths.document, "App");
      if (!appDir.exists) await appDir.create({ intermediates: true });

      // Create entry directory: App/2026-08-14_07-30-00/
      const entryDir = new Directory(appDir, timestampString);
      await entryDir.create({ intermediates: true });

      // Create images directory: App/2026-08-14_07-30-00/imgs/
      const imgsDir = new Directory(entryDir, "imgs");
      await imgsDir.create({ intermediates: true });

      // 2. Save image files into imgs/
      const savedImageFilenames: string[] = [];
      for (let i = 0; i < imageUris.length; i++) {
        const sourceFile = new File(imageUris[i]);
        const filename = `img_${i}_${Date.now()}.jpg`;
        const destFile = new File(imgsDir, filename);

        await sourceFile.copy(destFile);
        savedImageFilenames.push(filename);
      }

      // 3. Save JSON metadata into App/2026-08-14_07-30-00/data.json
      const jsonData = {
        id: Date.now(),
        title,
        text,
        date: date.toISOString(),
        images: savedImageFilenames, // Stores relative image filenames
      };

      const jsonFile = new File(entryDir, "data.json");
      await jsonFile.write(JSON.stringify(jsonData, null, 2));

      router.push("/");
    } catch (error) {
      console.error("Failed to save entry:", error);
    }
  };

  const formatDateLabel = (d: Date) => {
    return d.toLocaleDateString("en-US", {
      weekday: "short",
      month: "short",
      day: "numeric",
    });
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: themeColors.bg }]}>
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        style={styles.keyboardAvoidingView}
      >
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()}>
            <Ionicons name="chevron-back" size={24} color={themeColors.text} />
          </TouchableOpacity>

          <TouchableOpacity onPress={() => setShowDatePicker(true)}>
            <Text style={[styles.headerDate, { color: themeColors.text }]}>
              {formatDateLabel(date)}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity onPress={submit}>
            <Text style={[styles.doneButtonText, { color: themeColors.accent }]}>Done</Text>
          </TouchableOpacity>
        </View>

        <KeyboardAwareScrollView contentContainerStyle={styles.scrollContent}>
          {/* Photo Gallery Grid */}
          <View style={styles.galleryContainer}>
            {imageUris.map((uri, index) => (
              <View
                key={index}
                style={[
                  styles.imageWrapper,
                  imageUris.length === 1 ? styles.fullWidthImage : styles.gridImage,
                ]}
              >
                <Image source={{ uri }} style={styles.image} resizeMode="cover" />
                <TouchableOpacity style={styles.removeBtn} onPress={() => removeImage(index)}>
                  <Ionicons name="close" size={14} color="#555" />
                </TouchableOpacity>
              </View>
            ))}

            <TouchableOpacity
              style={[
                styles.addImageTile,
                imageUris.length === 0 ? styles.fullWidthImage : styles.gridImage,
                { backgroundColor: themeColors.cardBg },
              ]}
              onPress={pickImages}
            >
              <Ionicons name="camera-outline" size={28} color={themeColors.secondaryText} />
              <Text style={{ color: themeColors.secondaryText, marginTop: 4, fontSize: 12 }}>
                Add Photos
              </Text>
            </TouchableOpacity>
          </View>

          {/* Inputs */}
          <TextInput
            placeholder="Title (Optional)"
            placeholderTextColor={themeColors.placeholder}
            value={title}
            onChangeText={setTitle}
            style={[styles.titleInput, { color: themeColors.text }]}
          />

          <TextInput
            multiline
            placeholder="Write about your day..."
            placeholderTextColor={themeColors.placeholder}
            value={text}
            onChangeText={setText}
            style={[styles.noteInput, { color: themeColors.text }]}
          />
        </KeyboardAwareScrollView>
      </KeyboardAvoidingView>

      {/* Date Picker Modal */}
      {showDatePicker && (
        <Modal transparent animationType="fade" visible={showDatePicker}>
          <TouchableOpacity
            style={styles.modalOverlay}
            activeOpacity={1}
            onPress={() => setShowDatePicker(false)}
          >
            <View style={[styles.modalContent, { backgroundColor: themeColors.cardBg }]}>
              <DateTimePicker
                value={date}
                mode="date"
                display="inline"
                onChange={(event, selectedDate) => {
                  if (selectedDate) setDate(selectedDate);
                  setShowDatePicker(false);
                }}
              />
            </View>
          </TouchableOpacity>
        </Modal>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  keyboardAvoidingView: { flex: 1 },
  header: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  headerDate: { fontSize: 17, fontWeight: "700" },
  doneButtonText: { fontSize: 17, fontWeight: "600" },
  scrollContent: { padding: 16 },
  galleryContainer: { flexDirection: "row", flexWrap: "wrap", gap: 8, marginBottom: 20 },
  imageWrapper: { borderRadius: 16, overflow: "hidden", position: "relative", height: 160 },
  fullWidthImage: { width: "100%", height: 220 },
  gridImage: { width: "48.5%" },
  image: { width: "100%", height: "100%" },
  removeBtn: {
    position: "absolute",
    top: 8,
    right: 8,
    backgroundColor: "rgba(255, 255, 255, 0.8)",
    borderRadius: 12,
    padding: 4,
  },
  addImageTile: { borderRadius: 16, height: 160, justifyContent: "center", alignItems: "center" },
  titleInput: { fontSize: 22, fontWeight: "700", marginBottom: 12 },
  noteInput: { fontSize: 16, lineHeight: 24, minHeight: 180, textAlignVertical: "top" },
  modalOverlay: { flex: 1, backgroundColor: "rgba(0,0,0,0.4)", justifyContent: "center", alignItems: "center" },
  modalContent: { borderRadius: 16, padding: 16, width: "85%" },
});