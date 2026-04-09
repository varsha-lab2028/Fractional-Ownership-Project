package edu.iiitd.dbms.service;

import edu.iiitd.dbms.data_access.IpoDAO;
import edu.iiitd.dbms.data_access.ValuationDAO;
import edu.iiitd.dbms.dto.InvestorMarketView.MarketViewRow;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that drives simulated live market prices for all IPO assets.
 *
 * Behaviour:
 *   - Admin calls openMarket() / closeMarket()  (e.g. from AdminDashUI)
 *   - When open, call tick() manually to generate a new price update
 *     using a bounded random walk:  max ±2% per tick, hard cap ±30% from IPO base
 *   - Fires "priceUpdate" PropertyChangeEvent to registered listeners on every tick
 *   - Fires "marketState" PropertyChangeEvent when market opens/closes
 *   - Each tick writes a new row to the `valuation` table so the DB stays in sync
 *   - When admin verifies a new IPO, call addAsset() to start tracking it
 */
public class LiveMarketDataManager {

    // ── Tuning constants ────────────────────────────────────────────────────
    private static final double MAX_TICK_PCT  = 0.020;   // ±2% max move per tick
    private static final double DRIFT_CAP_PCT = 0.30;    // hard cap ±30% from IPO base
    public static final int     MAX_CANDLES   = 300;     // rolling window per asset

    // ── Singleton ───────────────────────────────────────────────────────────
    private static volatile LiveMarketDataManager instance;
    public static LiveMarketDataManager getInstance() {
        if (instance == null) {
            synchronized (LiveMarketDataManager.class) {
                if (instance == null) instance = new LiveMarketDataManager();
            }
        }
        return instance;
    }

    // ── State ────────────────────────────────────────────────────────────────
    /** IPO / initial price anchor per asset. Never changes after load. */
    private final Map<Integer, Double> basePrices    = new ConcurrentHashMap<>();
    /** Current live price per asset (updated every tick). */
    private final Map<Integer, Double> currentPrices = new ConcurrentHashMap<>();
    /** Human-readable name per asset. */
    private final Map<Integer, String> assetNames    = new ConcurrentHashMap<>();
    /**
     * Rolling candle history per asset.
     * Each double[6] = { timestamp_ms, open, high, low, close, volume }
     */
    private final Map<Integer, List<double[]>> candleData = new ConcurrentHashMap<>();

    private volatile boolean marketOpen = false;
    private final Random rng = new Random();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // DAOs — only used if a DB connection is available; failures are logged but non-fatal
    private IpoDAO       ipoDAO;
    private ValuationDAO valuationDAO;

    // ── Constructor ──────────────────────────────────────────────────────────
    private LiveMarketDataManager() {
        try { ipoDAO       = new IpoDAO();       } catch (Exception e) { ipoDAO       = null; }
        try { valuationDAO = new ValuationDAO();  } catch (Exception e) { valuationDAO = null; }
        loadInitialPrices();
    }

    // ── Initialisation ───────────────────────────────────────────────────────
    private void loadInitialPrices() {
        if (ipoDAO == null) return;
        try {
            List<MarketViewRow> rows = ipoDAO.getAllMarketRows();
            for (MarketViewRow row : rows) {
                int    assetId = row.getAssetId();
                double price   = row.getPricePerUnit();
                registerAsset(assetId, row.getAssetName(), price);
            }
        } catch (Exception e) {
            System.err.println("[LiveMarket] Could not load initial prices: " + e.getMessage());
        }
    }

    private void registerAsset(int assetId, String name, double ipoPrice) {
        basePrices.put(assetId, ipoPrice);
        currentPrices.put(assetId, ipoPrice);
        assetNames.put(assetId, name != null ? name : "Asset " + assetId);
        List<double[]> candles = new ArrayList<>();
        long now = System.currentTimeMillis();
        // Seed with 60 historical candles so chart looks full on first open
        double p = ipoPrice;
        for (int i = 60; i >= 1; i--) {
            double o = p;
            double c = o * (1 + (rng.nextDouble() * 0.04 - 0.02));
            double h = Math.max(o, c) * (1 + rng.nextDouble() * 0.005);
            double l = Math.min(o, c) * (1 - rng.nextDouble() * 0.005);
            candles.add(new double[]{now - (long) i * 2_000L, o, h, l, c, 500 + rng.nextDouble() * 1500});
            p = c;
        }
        currentPrices.put(assetId, p); // last historical close becomes starting price
        candleData.put(assetId, candles);
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /** Called by Admin UI when a new IPO is verified — adds it to the live feed. */
    public void addAsset(int assetId, String name, double ipoPrice) {
        if (!currentPrices.containsKey(assetId)) {
            registerAsset(assetId, name, ipoPrice);
        }
    }

    public synchronized void openMarket() {
        if (marketOpen) return;
        marketOpen = true;
        pcs.firePropertyChange("marketState", false, true);
        System.out.println("[LiveMarket] Market OPENED  (manual-tick mode)");
    }

    public synchronized void closeMarket() {
        if (!marketOpen) return;
        marketOpen = false;
        pcs.firePropertyChange("marketState", true, false);
        System.out.println("[LiveMarket] Market CLOSED");
    }

    public boolean isMarketOpen() { return marketOpen; }

    public double getCurrentPrice(int assetId) {
        return currentPrices.getOrDefault(assetId, 0.0);
    }

    public Map<Integer, Double> getAllCurrentPrices() {
        return Collections.unmodifiableMap(currentPrices);
    }

    /** Returns a snapshot of the candle data list (safe for chart rendering). */
    public List<double[]> getCandleSnapshot(int assetId) {
        List<double[]> src = candleData.get(assetId);
        return src == null ? Collections.emptyList() : new ArrayList<>(src);
    }

    public Map<Integer, String> getAssetNames() {
        return Collections.unmodifiableMap(assetNames);
    }

    public Set<Integer> getAssetIds() {
        return Collections.unmodifiableSet(currentPrices.keySet());
    }

    public void addListener(PropertyChangeListener l)    { pcs.addPropertyChangeListener(l); }
    public void removeListener(PropertyChangeListener l) { pcs.removePropertyChangeListener(l); }

    // ── Tick logic — call this manually (e.g. from a Refresh button) ─────────
    public void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Double> entry : currentPrices.entrySet()) {
            int    assetId = entry.getKey();
            double prev    = entry.getValue();
            double base    = basePrices.getOrDefault(assetId, prev);

            // Bounded random walk with mean-reversion at the caps
            double pct   = (rng.nextDouble() * 2.0 - 1.0) * MAX_TICK_PCT;
            double drift = (base > 0) ? (prev - base) / base : 0;
            if (drift >  DRIFT_CAP_PCT) pct -= 0.008;
            else if (drift < -DRIFT_CAP_PCT) pct += 0.008;

            double newPrice = Math.max(0.01, prev * (1.0 + pct));
            currentPrices.put(assetId, newPrice);

            // Append candle
            double high = Math.max(prev, newPrice) * (1.0 + rng.nextDouble() * 0.004);
            double low  = Math.min(prev, newPrice) * (1.0 - rng.nextDouble() * 0.004);
            double vol  = 500 + rng.nextDouble() * 2000;
            List<double[]> candles = candleData.computeIfAbsent(assetId, k -> new ArrayList<>());
            candles.add(new double[]{now, prev, high, low, newPrice, vol});
            if (candles.size() > MAX_CANDLES) candles.remove(0);

            // Persist to DB (non-fatal if connection unavailable)
            if (valuationDAO != null) {
                try {
                    valuationDAO.insertValuationToSimulate(assetId, newPrice, new Date(now));
                } catch (Exception ignored) {}
            }
        }
        // Fire single event carrying the full price map snapshot
        pcs.firePropertyChange("priceUpdate", null, new HashMap<>(currentPrices));
    }
}