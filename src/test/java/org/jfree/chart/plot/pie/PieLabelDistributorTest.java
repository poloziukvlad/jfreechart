/* ===========================================================
 * Характеризаційні тести для PieLabelDistributor (ПР №3)
 * Методика Майкла Фезерса: викликати код -> зафіксувати
 * фактичний результат -> оформити як твердження.
 * Тести розміщені в пакеті org.jfree.chart.plot.pie, щоб мати
 * доступ до protected-методів (adjustInwards тощо).
 * =========================================================== */
package org.jfree.chart.plot.pie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PieLabelDistributorTest {

    /** Межа похибки для порівняння double. */
    private static final double EPS = 1e-6;

    /** Створює запис підпису; TextBox=null (дистриб'ютор використовує лише
     *  baseY/labelHeight/allocatedY). */
    private static PieLabelRecord rec(Comparable key, double baseY, double h) {
        return new PieLabelRecord(key, 0.0, baseY, null, h, 0.0, 0.0);
    }

    /** Чи є перекриття у поточному розкладанні (допоміжний інваріант). */
    private static boolean hasOverlap(PieLabelDistributor d) {
        double y = 0.0;
        for (int i = 0; i < d.getItemCount(); i++) {
            PieLabelRecord r = d.getPieLabelRecord(i);
            if (y > r.getLowerY()) return true;
            y = r.getUpperY();
        }
        return false;
    }

    // ---------- Типові сценарії ----------

    @Test
    @DisplayName("Порожній список: distributeLabels не падає і нічого не додає")
    void emptyList() {
        PieLabelDistributor d = new PieLabelDistributor(5);
        d.distributeLabels(0, 100);
        assertEquals(0, d.getItemCount());
    }

    @Test
    @DisplayName("Один запис: координата не змінюється")
    void singleLabelUnchanged() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        d.addPieLabelRecord(rec("A", 50, 20));
        d.distributeLabels(0, 100);
        assertEquals(50.0, d.getPieLabelRecord(0).getAllocatedY(), EPS);
    }

    @Test
    @DisplayName("Два записи без перекриття: координати не змінюються")
    void twoLabelsNoOverlap() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        d.addPieLabelRecord(rec("A", 10, 10));
        d.addPieLabelRecord(rec("B", 50, 10));
        d.distributeLabels(0, 100);
        assertEquals(10.0, d.getPieLabelRecord(0).getAllocatedY(), EPS);
        assertEquals(50.0, d.getPieLabelRecord(1).getAllocatedY(), EPS);
        assertFalse(hasOverlap(d));
    }

    @Test
    @DisplayName("Вхід не відсортований: sort() упорядковує за baseY")
    void sortsByBaseY() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        d.addPieLabelRecord(rec("B", 80, 10));
        d.addPieLabelRecord(rec("A", 20, 10));
        d.distributeLabels(0, 100);
        assertEquals("A", d.getPieLabelRecord(0).getKey());
        assertEquals("B", d.getPieLabelRecord(1).getKey());
    }

    // ---------- Перекриття / основна логіка ----------

    @Test
    @DisplayName("Два записи з перекриттям: adjustDownwards зсуває другий до allocY=34.0")
    void twoLabelsOverlapAdjustedDownwards() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        d.addPieLabelRecord(rec("A", 10, 20));
        d.addPieLabelRecord(rec("B", 15, 20));
        d.distributeLabels(0, 100);
        assertEquals(10.0, d.getPieLabelRecord(0).getAllocatedY(), EPS);
        assertEquals(34.0, d.getPieLabelRecord(1).getAllocatedY(), EPS);
        assertFalse(hasOverlap(d));
    }

    @Test
    @DisplayName("adjustInwards() напряму (3 записи): B->34.0, C->58.0")
    void adjustInwardsThreeRecords() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        d.addPieLabelRecord(rec("A", 10, 20));
        d.addPieLabelRecord(rec("B", 15, 20));
        d.addPieLabelRecord(rec("C", 20, 20));
        d.sort();
        d.adjustInwards();
        assertEquals(10.0, d.getPieLabelRecord(0).getAllocatedY(), EPS);
        assertEquals(34.0, d.getPieLabelRecord(1).getAllocatedY(), EPS);
        assertEquals(58.0, d.getPieLabelRecord(2).getAllocatedY(), EPS);
    }

    @Test
    @DisplayName("adjustInwards() з 2 записами: гілка lower<upper-1 не виконується")
    void adjustInwardsTwoRecords() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        d.addPieLabelRecord(rec("A", 10, 20));
        d.addPieLabelRecord(rec("B", 15, 20));
        d.sort();
        d.adjustInwards();
        assertEquals(34.0, d.getPieLabelRecord(1).getAllocatedY(), EPS);
    }

    // ---------- Межові випадки ----------

    @Test
    @DisplayName("ХАРАКТЕРИЗАЦІЯ (підозріло): при height < суми висот spreadEvenly НЕ усуває перекриття")
    void tightSpaceStillOverlaps() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        for (int i = 0; i < 4; i++) {
            d.addPieLabelRecord(rec("K" + i, 10 + i * 2, 20));
        }
        // сума висот = 80, а доступна висота лише 30
        d.distributeLabels(0, 30);
        assertEquals(10.0, d.getPieLabelRecord(0).getAllocatedY(), EPS);
        assertEquals(20.0, d.getPieLabelRecord(3).getAllocatedY(), EPS);
        // ХАРАКТЕРИЗАЦІЯ: підозріло — можливий дефект. "Останній засіб"
        // spreadEvenly дає від'ємний gap і лишає підписи накладеними.
        assertTrue(hasOverlap(d),
            "Зафіксовано фактичну поведінку: перекриття лишається");
    }

    @Test
    @DisplayName("toString() повертає 'baseY, key' з переносом рядка")
    void toStringFormat() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        d.addPieLabelRecord(rec("X", 5, 4));
        assertEquals("5.0, X\n", d.toString());
    }

    // ---------- Обробка помилок ----------

    @Test
    @DisplayName("addPieLabelRecord(null) кидає IllegalArgumentException")
    void nullRecordThrows() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        assertThrows(IllegalArgumentException.class,
                () -> d.addPieLabelRecord(null));
    }

    @Test
    @DisplayName("clear() очищає список записів")
    void clearEmptiesList() {
        PieLabelDistributor d = new PieLabelDistributor(0);
        d.addPieLabelRecord(rec("A", 1, 1));
        assertEquals(1, d.getItemCount());
        d.clear();
        assertEquals(0, d.getItemCount());
    }
}
