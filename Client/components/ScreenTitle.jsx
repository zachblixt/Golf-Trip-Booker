import { StyleSheet, Text, View } from 'react-native';

import RoleBadge from './RoleBadge';
import { type } from '../theme';

/**
 * A screen's heading, with the signed-in role beside it.
 *
 * A component rather than a badge pasted into six title rows: putting it here means
 * every tab shows the same thing in the same place, and a screen added later gets
 * the badge by using the same title everyone else uses. Forgetting is the failure
 * mode, and this is the version that cannot be forgotten halfway.
 */
export default function ScreenTitle({ children }) {
    return (
        <View style={styles.row}>
            <Text style={[type.title, styles.title]}>{children}</Text>
            <RoleBadge />
        </View>
    );
}

const styles = StyleSheet.create({
    row: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 12,
    },
    // Lets a long title wrap or shrink instead of shoving the badge off the edge.
    title: { flexShrink: 1 },
});
