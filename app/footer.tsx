import { FontAwesome5 } from "@expo/vector-icons";
import { usePathname, useRouter } from "expo-router";
import React from "react";
import { Pressable, StyleSheet, Text, useColorScheme, View } from "react-native";
import { Colors } from "./theme";

export default function Footer() {
  const router = useRouter();
  const pathname = usePathname();
  const colorScheme = useColorScheme();
  const theme = Colors[colorScheme ?? 'light'];

  // Helper to determine active state and return matching color
  const getTabColor = (path: string) => {
    return pathname === path ? theme.accent : theme.subtext;
  };

  return (
    <View style={[styles.footer, { backgroundColor: theme.card, borderTopColor: theme.border }]}>
      {/* My Tasks Tab */}
      <Pressable onPress={() => router.push('/')} style={styles.tab}>
        <FontAwesome5
          name="list-ul"
          size={20}
          color={getTabColor('/')}
        />
        <Text style={[styles.tabLabel, { color: getTabColor('/') }]}>
          Tasks
        </Text>
      </Pressable>

      {/* Transactions Tab */}
      <Pressable onPress={() => router.push('/diary/diary')} style={styles.tab}>
        <FontAwesome5
          name="book"
          size={20}
          color={getTabColor('/diary/diary')}
        />
        <Text style={[styles.tabLabel, { color: getTabColor('/diary/diary') }]}>
          Diary
        </Text>
      </Pressable>

      {/* Transactions Tab */}
      <Pressable onPress={() => router.push('/expense_tracker/transactions')} style={styles.tab}>
        <FontAwesome5
          name="dollar-sign"
          size={20}
          color={getTabColor('/expense_tracker/transactions')}
        />
        <Text style={[styles.tabLabel, { color: getTabColor('/expense_tracker/transactions') }]}>
          Cash
        </Text>
      </Pressable>

      {/* My Notes Tab */}
      <Pressable onPress={() => router.push('/notes/notes')} style={styles.tab}>
        <FontAwesome5
          name="sticky-note"
          size={20}
          color={getTabColor('/notes/notes')}
        />
        <Text style={[styles.tabLabel, { color: getTabColor('/notes/notes') }]}>
          Notes
        </Text>
      </Pressable>

      {/* Password Tab */}
      <Pressable onPress={() => router.push('/settings')} style={styles.tab}>
        <FontAwesome5
          name="cog"
          size={20}
          color={getTabColor('/settings')}
        />
        <Text style={[styles.tabLabel, { color: getTabColor('/settings') }]}>
          Settings
        </Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingTop: 8,
    paddingBottom: 15,
    borderTopWidth: 1,
    position: 'absolute',
    bottom: 0,
    width: '100%',
    shadowColor: "#000",
    shadowOffset: { width: 0, height: -2 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 5,
  },
  tab: {
    alignItems: 'center',
    justifyContent: 'center',
    minWidth: 70,
    paddingVertical: 4,
  },
  tabLabel: {
    fontSize: 11,
    marginTop: 4,
    fontWeight: '500',
  },
});