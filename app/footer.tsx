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
      <Pressable onPress={() => router.push('/transactions')} style={styles.tab}>
        <FontAwesome5 
          name="dollar-sign" 
          size={20} 
          color={getTabColor('/transactions')} 
        />
        <Text style={[styles.tabLabel, { color: getTabColor('/transactions') }]}>
          Transactions
        </Text>
      </Pressable>

       {/* My Notes Tab */}
      <Pressable onPress={() => router.push('/notes')} style={styles.tab}>
        <FontAwesome5 
          name="sticky-note" 
          size={20} 
          color={getTabColor('/notes')} 
        />
        <Text style={[styles.tabLabel, { color: getTabColor('/notes') }]}>
          Notes
        </Text>
      </Pressable>

      {/* Password Tab */}
      <Pressable onPress={() => router.push('/password')} style={styles.tab}>
        <FontAwesome5 
          name="key" 
          size={20} 
          color={getTabColor('/password')} 
        />
        <Text style={[styles.tabLabel, { color: getTabColor('/password') }]}>
          Passwords
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