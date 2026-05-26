const pptxgen = require("/home/claude/.npm-global/lib/node_modules/pptxgenjs");
const path = require("path");

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9";
pres.title = "A Multi-Modal AI Framework for Real-Time Tea Recognition and Personalized Nutritional Analytics";

// Color palette — fresh, tea-inspired light theme
const C = {
  white:    "FFFFFF",
  offWhite: "F8FAF5",
  lightGreen: "EAF4E8",
  midGreen: "C8E6C0",
  darkGreen: "2E7D32",
  accentGreen: "43A047",
  teal:     "00796B",
  lightTeal: "E0F2F1",
  gold:     "F9A825",
  lightGold:"FFF8E1",
  text:     "1B2E1B",
  subtext:  "4E6450",
  muted:    "7A9A7C",
  white2:   "F0F7EE",
  cardBg:   "FFFFFF",
  border:   "C8E6C0",
};

// Helper: slide background
function setBg(slide, color) {
  slide.background = { color };
}

// Helper: title text
function addTitle(slide, txt, opts = {}) {
  slide.addText(txt, {
    x: opts.x ?? 0.45,
    y: opts.y ?? 0.2,
    w: opts.w ?? 9.1,
    h: opts.h ?? 0.75,
    fontSize: opts.fontSize ?? 28,
    bold: true,
    color: opts.color ?? C.darkGreen,
    fontFace: "Calibri",
    align: opts.align ?? "left",
    margin: 0,
  });
}

// Helper: body text
function addBody(slide, txt, x, y, w, h, opts = {}) {
  slide.addText(txt, {
    x, y, w, h,
    fontSize: opts.fontSize ?? 13,
    color: opts.color ?? C.text,
    fontFace: "Calibri",
    align: opts.align ?? "left",
    valign: opts.valign ?? "top",
    bold: opts.bold ?? false,
    italic: opts.italic ?? false,
  });
}

// Helper: card (rounded rect with shadow)
function addCard(slide, x, y, w, h, fillColor) {
  slide.addShape(pres.shapes.RECTANGLE, {
    x, y, w, h,
    fill: { color: fillColor ?? C.white },
    line: { color: C.border, width: 1 },
    shadow: { type: "outer", color: "000000", blur: 8, offset: 2, angle: 135, opacity: 0.08 },
  });
}

// Helper: accent left border
function addAccentBar(slide, x, y, h, color) {
  slide.addShape(pres.shapes.RECTANGLE, {
    x, y, w: 0.07, h,
    fill: { color: color ?? C.accentGreen },
    line: { color: color ?? C.accentGreen, width: 0 },
  });
}

// Helper: numbered circle
function addNumberCircle(slide, num, x, y, color) {
  slide.addShape(pres.shapes.OVAL, {
    x, y, w: 0.38, h: 0.38,
    fill: { color: color ?? C.accentGreen },
    line: { color: color ?? C.accentGreen, width: 0 },
  });
  slide.addText(String(num), {
    x, y, w: 0.38, h: 0.38,
    fontSize: 13, bold: true, color: C.white,
    fontFace: "Calibri", align: "center", valign: "middle",
  });
}

// ─────────────────────────────────────────────
// SLIDE 1 — Title
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.darkGreen);

  // Left accent strip
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 0.25, h: 5.625, fill: { color: C.gold }, line: { color: C.gold } });

  // Right image area
  s.addShape(pres.shapes.RECTANGLE, { x: 6.5, y: 0, w: 3.5, h: 5.625, fill: { color: "1B5E20" }, line: { color: "1B5E20" } });

  // Tea cup icon placeholder (SVG circle + saucer suggestion)
  s.addShape(pres.shapes.OVAL, { x: 7.5, y: 0.8, w: 1.5, h: 1.5, fill: { color: C.accentGreen }, line: { color: C.midGreen, width: 2 } });
  s.addText("🍵", { x: 7.5, y: 0.8, w: 1.5, h: 1.5, fontSize: 48, align: "center", valign: "middle" });

  // Tea leaf shapes decorative
  s.addShape(pres.shapes.OVAL, { x: 7.2, y: 2.7, w: 0.5, h: 0.25, fill: { color: C.midGreen }, line: { color: C.midGreen }, rotate: 30 });
  s.addShape(pres.shapes.OVAL, { x: 8.0, y: 3.1, w: 0.6, h: 0.28, fill: { color: C.accentGreen }, line: { color: C.accentGreen }, rotate: -20 });
  s.addShape(pres.shapes.OVAL, { x: 7.6, y: 3.5, w: 0.45, h: 0.22, fill: { color: C.midGreen }, line: { color: C.midGreen }, rotate: 15 });

  // Main title
  s.addText("A Multi-Modal AI Framework for", {
    x: 0.5, y: 1.0, w: 6.0, h: 0.65,
    fontSize: 22, bold: false, color: C.midGreen,
    fontFace: "Calibri", align: "left",
  });
  s.addText("Real-Time Tea Recognition", {
    x: 0.5, y: 1.55, w: 6.0, h: 0.75,
    fontSize: 30, bold: true, color: C.white,
    fontFace: "Calibri", align: "left",
  });
  s.addText("and Personalized Nutritional Analytics", {
    x: 0.5, y: 2.2, w: 6.0, h: 0.65,
    fontSize: 22, bold: true, color: C.gold,
    fontFace: "Calibri", align: "left",
  });

  s.addShape(pres.shapes.LINE, { x: 0.5, y: 2.95, w: 5.6, h: 0, line: { color: C.midGreen, width: 1.2 } });

  s.addText("Presented By: Deepti Joshi  |  Enrollment: A501144824008", {
    x: 0.5, y: 3.1, w: 6.0, h: 0.38,
    fontSize: 13, color: C.midGreen, fontFace: "Calibri",
  });
  s.addText("Under Guidance: Dr. Paras Chawla  •  Dr. Shubham Mahajan", {
    x: 0.5, y: 3.48, w: 6.0, h: 0.38,
    fontSize: 13, color: C.midGreen, fontFace: "Calibri",
  });
  s.addText("Amity University Haryana  |  M.Tech Artificial Intelligence  |  2026", {
    x: 0.5, y: 3.88, w: 6.0, h: 0.38,
    fontSize: 12, color: C.muted, fontFace: "Calibri",
  });
}

// ─────────────────────────────────────────────
// SLIDE 2 — Problem Statement & Motivation
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Problem Statement & Motivation");

  // Left column — problem points
  const problems = [
    ["Manual Identification is Hard", "Tea varieties look visually similar; packaging differs across brands and regions."],
    ["Lab Tests Are Impractical", "Traditional spectroscopy-based methods require equipment and expertise."],
    ["Nutritional Awareness Gap", "Consumers lack real-time access to caffeine, antioxidant, and calorie data."],
    ["Single-Modality AI Fails", "Image-only models struggle with fine-grained classification under real-world conditions."],
  ];

  problems.forEach(([head, body], i) => {
    const y = 1.05 + i * 1.07;
    addCard(s, 0.45, y, 5.1, 0.9, C.white);
    addAccentBar(s, 0.45, y, 0.9, C.accentGreen);
    s.addText(head, { x: 0.65, y: y + 0.08, w: 4.8, h: 0.3, fontSize: 13, bold: true, color: C.darkGreen, fontFace: "Calibri" });
    s.addText(body, { x: 0.65, y: y + 0.38, w: 4.8, h: 0.42, fontSize: 11.5, color: C.subtext, fontFace: "Calibri" });
  });

  // Right column — stat callouts
  const stats = [
    ["18", "Tea Categories\nClassified"],
    ["5,400+", "Training Images\nDataset"],
    ["96.48%", "Best Model\nAccuracy"],
  ];
  stats.forEach(([num, label], i) => {
    const y = 1.15 + i * 1.3;
    addCard(s, 5.9, y, 3.7, 0.95, C.lightGreen);
    s.addText(num, { x: 5.9, y: y + 0.05, w: 1.4, h: 0.85, fontSize: 32, bold: true, color: C.darkGreen, fontFace: "Calibri", align: "center", valign: "middle" });
    s.addText(label, { x: 7.2, y: y + 0.2, w: 2.3, h: 0.55, fontSize: 12, color: C.subtext, fontFace: "Calibri", valign: "top" });
  });

  s.addText("Why This Research Matters", {
    x: 5.9, y: 4.75, w: 3.7, h: 0.38,
    fontSize: 11, italic: true, color: C.muted, fontFace: "Calibri", align: "center",
  });
}

// ─────────────────────────────────────────────
// SLIDE 3 — Research Objectives
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Research Objectives");

  const objs = [
    ["O1", "Construct a balanced dataset of ~5,400 images across 18 tea categories."],
    ["O2", "Develop & evaluate EfficientNetV2-S and MobileNetV3Small via transfer learning."],
    ["O3", "Design a hybrid ensemble with confidence-aware Top-3 prediction mechanism."],
    ["O4", "Integrate barcode scanning and OCR-assisted verification for robustness."],
    ["O5", "Deploy the optimized model as an Android app using TensorFlow Lite."],
    ["O6", "Provide nutritional info, health benefits, scan history, and analytics."],
    ["O7", "Evaluate using accuracy, precision, recall, F1-score, and confusion matrices."],
  ];

  const cols = [
    objs.slice(0, 4),
    objs.slice(4),
  ];

  cols.forEach((col, ci) => {
    const xBase = 0.45 + ci * 4.85;
    col.forEach(([id, text], ri) => {
      const y = 1.1 + ri * 1.02;
      addCard(s, xBase, y, 4.5, 0.85, C.white);
      // colored badge
      s.addShape(pres.shapes.RECTANGLE, {
        x: xBase, y, w: 0.55, h: 0.85,
        fill: { color: ci === 0 ? C.darkGreen : C.teal },
        line: { color: ci === 0 ? C.darkGreen : C.teal },
      });
      s.addText(id, {
        x: xBase, y, w: 0.55, h: 0.85,
        fontSize: 13, bold: true, color: C.white, fontFace: "Calibri",
        align: "center", valign: "middle",
      });
      s.addText(text, {
        x: xBase + 0.65, y: y + 0.1, w: 3.75, h: 0.65,
        fontSize: 11.5, color: C.text, fontFace: "Calibri", valign: "top",
      });
    });
  });

  // Key contribution box
  addCard(s, 0.45, 5.18, 9.1, 0.35, C.lightGold);
  s.addText("Key Contribution: First system combining multi-modal AI (Vision + OCR + Barcode) with confidence-aware analytics for real-time tea wellness support.", {
    x: 0.6, y: 5.2, w: 8.8, h: 0.3,
    fontSize: 11.5, bold: true, color: C.darkGreen, fontFace: "Calibri", align: "center",
  });
}

// ─────────────────────────────────────────────
// SLIDE 4 — Literature Review
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Literature Review");

  const refs = [
    ["Spectroscopy-Based", "Near-infrared and Raman spectroscopy achieve high tea discrimination but require specialized lab equipment — impractical for consumers."],
    ["Sensor-Based Systems", "Electronic noses and tongues provide useful signals but struggle with real-world packaging and brewed tea variations."],
    ["Classical ML Models", "SVM, KNN, and Random Forest on handcrafted features yield moderate accuracy (~75–85%); limited generalization to fine-grained classes."],
    ["Deep Learning CNNs", "ResNet, VGG, InceptionV3 showed improved accuracy on image datasets but lacked multi-modal verification strategies."],
    ["Hybrid & Ensemble Models", "Combining multiple CNN architectures marginally improved results; no prior work integrated barcode/OCR with confidence-aware decision support."],
    ["Transfer Learning Studies", "EfficientNet family demonstrated superior accuracy-to-cost ratio for fine-grained food classification tasks (benchmark: ~94–97%)."],
  ];

  refs.forEach(([head, body], i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.45 + col * 4.85;
    const y = 1.1 + row * 1.38;
    addCard(s, x, y, 4.5, 1.2, C.white);
    addAccentBar(s, x, y, 1.2, i < 2 ? C.darkGreen : i < 4 ? C.teal : C.gold);
    s.addText(head, { x: x + 0.2, y: y + 0.1, w: 4.2, h: 0.3, fontSize: 12.5, bold: true, color: C.darkGreen, fontFace: "Calibri" });
    s.addText(body, { x: x + 0.2, y: y + 0.38, w: 4.2, h: 0.75, fontSize: 11, color: C.subtext, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 5 — Research Gap
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Research Gap");

  const gaps = [
    ["No Multi-Modal Integration", "No prior study combines image AI, OCR, and barcode scanning into a unified tea recognition framework."],
    ["No Confidence-Aware Mechanism", "Existing systems output a single prediction without providing alternative options or uncertainty handling."],
    ["Lack of Cross-Validation on Tea Data", "No cross-dataset or large-scale fine-grained validation across 18+ tea categories under identical conditions."],
    ["No Consumer Nutritional Analytics", "Previous models only classify; none integrate a real-time nutritional knowledge base or personalized health insights."],
    ["No Mobile Deployment", "Most studies remain as desktop/notebook experiments without TFLite Android deployment for real-world use."],
    ["Insufficient Cost Analysis", "No study provides computational cost comparison across multi-modal modalities for mobile hardware constraints."],
  ];

  gaps.forEach(([head, body], i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.45 + col * 4.85;
    const y = 1.08 + row * 1.38;
    addCard(s, x, y, 4.5, 1.22, C.white);
    // circle number
    addNumberCircle(s, i + 1, x + 0.1, y + 0.42, i % 3 === 0 ? C.darkGreen : i % 3 === 1 ? C.teal : C.gold);
    s.addText(head, { x: x + 0.62, y: y + 0.08, w: 3.75, h: 0.32, fontSize: 12.5, bold: true, color: C.darkGreen, fontFace: "Calibri" });
    s.addText(body, { x: x + 0.62, y: y + 0.4, w: 3.75, h: 0.72, fontSize: 11, color: C.subtext, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 6 — Proposed Methodology Workflow
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Proposed Methodology Workflow");

  const steps = [
    { label: "User Input", sub: "Camera / Gallery\nor Barcode Scan", color: C.darkGreen, x: 0.3, y: 1.3 },
    { label: "Pre-processing", sub: "Resize 224×224\nNormalize & Augment", color: C.accentGreen, x: 2.15, y: 1.3 },
    { label: "Multi-Modal\nInference", sub: "EfficientNetV2-S\n+ MobileNetV3 + OCR", color: C.teal, x: 4.0, y: 1.3 },
    { label: "Confidence\nDecision", sub: "Top-3 Predictions\n& Manual Fallback", color: C.gold, x: 5.85, y: 1.3 },
    { label: "Analytics\nOutput", sub: "Nutrition DB +\nHealth Benefits", color: C.accentGreen, x: 7.7, y: 1.3 },
  ];

  steps.forEach((st, i) => {
    addCard(s, st.x, st.y, 1.7, 1.6, C.white);
    s.addShape(pres.shapes.RECTANGLE, {
      x: st.x, y: st.y, w: 1.7, h: 0.45,
      fill: { color: st.color }, line: { color: st.color },
    });
    s.addText(String(i + 1), {
      x: st.x, y: st.y, w: 1.7, h: 0.45,
      fontSize: 18, bold: true, color: C.white, fontFace: "Calibri", align: "center", valign: "middle",
    });
    s.addText(st.label, {
      x: st.x + 0.05, y: st.y + 0.5, w: 1.6, h: 0.45,
      fontSize: 11.5, bold: true, color: C.darkGreen, fontFace: "Calibri", align: "center",
    });
    s.addText(st.sub, {
      x: st.x + 0.05, y: st.y + 0.93, w: 1.6, h: 0.58,
      fontSize: 10, color: C.subtext, fontFace: "Calibri", align: "center",
    });
    if (i < steps.length - 1) {
      s.addShape(pres.shapes.LINE, {
        x: st.x + 1.72, y: st.y + 0.8, w: 0.4, h: 0,
        line: { color: C.accentGreen, width: 2 },
      });
      s.addText("▶", { x: st.x + 1.98, y: st.y + 0.65, w: 0.2, h: 0.3, fontSize: 12, color: C.accentGreen, fontFace: "Calibri", align: "center" });
    }
  });

  // Supporting details row
  const details = [
    ["Dataset", "~5,400 images\n18 categories\nBalanced split (80:20)"],
    ["Models", "EfficientNetV2-S (primary)\nMobileNetV3Small (lightweight)\nHybrid Ensemble"],
    ["Modalities", "Image Classification\nOCR Text Extraction\nBarcode Lookup"],
    ["Deployment", "TFLite → Android App\nKotlin + CameraX\nGoogle ML Kit"],
  ];

  details.forEach((d, i) => {
    const x = 0.45 + i * 2.4;
    addCard(s, x, 3.15, 2.2, 2.2, C.lightGreen);
    s.addText(d[0], { x: x + 0.1, y: 3.22, w: 2.0, h: 0.35, fontSize: 12.5, bold: true, color: C.darkGreen, fontFace: "Calibri" });
    s.addText(d[1], { x: x + 0.1, y: 3.6, w: 2.0, h: 1.65, fontSize: 11, color: C.subtext, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 7 — Dataset Description
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Dataset Description");

  // Chart — class distribution (sample of 9 categories)
  const categories = ["Green Tea", "Black Tea", "Oolong", "Matcha", "Herbal", "White Tea", "Chamomile", "Ginger", "Lemon Tea"];
  const values = [302, 298, 295, 301, 300, 297, 299, 296, 303];

  s.addChart(pres.charts.BAR, [{ name: "Images", labels: categories, values }], {
    x: 0.45, y: 1.0, w: 5.8, h: 4.3,
    barDir: "col",
    chartColors: ["2E7D32", "43A047", "66BB6A", "00796B", "26A69A", "4DB6AC", "F9A825", "FB8C00", "EF6C00"],
    chartArea: { fill: { color: C.white } },
    catAxisLabelColor: C.subtext,
    valAxisLabelColor: C.subtext,
    valGridLine: { color: "E8F5E9", size: 0.8 },
    catGridLine: { style: "none" },
    showValue: true,
    dataLabelColor: "1B2E1B",
    dataLabelFontSize: 9,
    dataLabelPosition: "outEnd",
    showLegend: false,
    showTitle: true,
    title: "Sample Category Distribution (9 of 18)",
    titleColor: C.darkGreen,
    titleFontSize: 12,
  });

  // Right side — key stats
  const stats2 = [
    ["~5,400", "Total Images"],
    ["18", "Tea Categories"],
    ["~300", "Images / Class"],
    ["80:20", "Train / Val Split"],
    ["224×224", "Input Resolution"],
  ];
  stats2.forEach(([val, lbl], i) => {
    const y = 1.05 + i * 0.92;
    addCard(s, 6.6, y, 3.0, 0.75, i % 2 === 0 ? C.lightGreen : C.lightTeal);
    s.addText(val, { x: 6.6, y: y + 0.04, w: 1.2, h: 0.67, fontSize: 22, bold: true, color: C.darkGreen, fontFace: "Calibri", align: "center", valign: "middle" });
    s.addText(lbl, { x: 7.85, y: y + 0.18, w: 1.65, h: 0.4, fontSize: 12, color: C.subtext, fontFace: "Calibri", valign: "middle" });
  });

  s.addText("Data collected via automated web crawl + quality filtering + manual validation", {
    x: 0.45, y: 5.3, w: 9.1, h: 0.25,
    fontSize: 11, italic: true, color: C.muted, fontFace: "Calibri", align: "center",
  });
}

// ─────────────────────────────────────────────
// SLIDE 8 — Preprocessing Pipeline
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Data Preprocessing Pipeline");

  const pipeline = [
    { step: "1", name: "Web Collection", desc: "Automated crawling, URL dedup, and candidate image download from online sources.", color: C.darkGreen },
    { step: "2", name: "Quality Filter", desc: "Blur detection, lighting assessment, CLIP-based relevance filtering, perceptual hash dedup.", color: C.accentGreen },
    { step: "3", name: "Manual Validation", desc: "Human review of edge cases, label correction, and removal of mislabeled samples.", color: C.teal },
    { step: "4", name: "Resize & Normalize", desc: "All images resized to 224×224 RGB. Pixel values normalized per EfficientNet/MobileNet spec.", color: C.gold },
    { step: "5", name: "Augmentation", desc: "Random rotation ±20°, horizontal flip, zoom 0.85–1.15×, brightness ±0.2.", color: C.accentGreen },
    { step: "6", name: "Balanced Dataset", desc: "Class balancing applied. Final: ~300 images/class, 80% train / 20% validation split.", color: C.darkGreen },
  ];

  pipeline.forEach((p, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.45 + col * 4.85;
    const y = 1.1 + row * 1.45;
    addCard(s, x, y, 4.5, 1.28, C.white);
    s.addShape(pres.shapes.RECTANGLE, {
      x, y, w: 0.5, h: 1.28,
      fill: { color: p.color }, line: { color: p.color },
    });
    s.addText(p.step, {
      x, y, w: 0.5, h: 1.28,
      fontSize: 20, bold: true, color: C.white, fontFace: "Calibri", align: "center", valign: "middle",
    });
    s.addText(p.name, { x: x + 0.62, y: y + 0.1, w: 3.75, h: 0.35, fontSize: 13, bold: true, color: C.darkGreen, fontFace: "Calibri" });
    s.addText(p.desc, { x: x + 0.62, y: y + 0.45, w: 3.75, h: 0.72, fontSize: 11, color: C.subtext, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 9 — Model Architecture (EfficientNetV2-S)
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Model Architecture: EfficientNetV2-S");

  // Left — architecture flow
  const layers = [
    { name: "Input Layer", detail: "224×224×3 RGB", color: C.darkGreen },
    { name: "Pretrained EfficientNetV2-S", detail: "ImageNet weights (frozen base)", color: C.accentGreen },
    { name: "Global Avg Pooling", detail: "Spatial feature compression", color: C.teal },
    { name: "Dense + Dropout", detail: "256 units, dropout 0.3", color: C.teal },
    { name: "Output Layer", detail: "18 neurons, Softmax", color: C.gold },
  ];

  layers.forEach((l, i) => {
    const y = 1.08 + i * 0.82;
    addCard(s, 0.45, y, 4.1, 0.65, C.white);
    s.addShape(pres.shapes.RECTANGLE, {
      x: 0.45, y, w: 0.45, h: 0.65,
      fill: { color: l.color }, line: { color: l.color },
    });
    s.addText(String(i + 1), {
      x: 0.45, y, w: 0.45, h: 0.65,
      fontSize: 14, bold: true, color: C.white, fontFace: "Calibri", align: "center", valign: "middle",
    });
    s.addText(l.name, { x: 1.02, y: y + 0.04, w: 3.42, h: 0.28, fontSize: 12.5, bold: true, color: C.darkGreen, fontFace: "Calibri" });
    s.addText(l.detail, { x: 1.02, y: y + 0.32, w: 3.42, h: 0.26, fontSize: 11, color: C.subtext, fontFace: "Calibri" });
    if (i < layers.length - 1) {
      s.addText("↓", { x: 1.7, y: y + 0.66, w: 0.5, h: 0.18, fontSize: 12, color: C.accentGreen, fontFace: "Calibri", align: "center" });
    }
  });

  // Right — training config
  const params = [
    ["Optimizer", "Adam (lr=0.001)"],
    ["Loss Function", "Categorical Cross-Entropy"],
    ["Epochs", "15 with Early Stopping"],
    ["Batch Size", "32"],
    ["Regularization", "Batch Norm + Dropout 0.3"],
    ["LR Scheduler", "ReduceLROnPlateau"],
    ["Base Weights", "ImageNet Pretrained"],
    ["Fine-tuning", "Top layers unfrozen after E5"],
  ];

  addCard(s, 5.0, 1.0, 4.6, 4.5, C.lightGreen);
  s.addText("Training Configuration", {
    x: 5.1, y: 1.08, w: 4.4, h: 0.35,
    fontSize: 13, bold: true, color: C.darkGreen, fontFace: "Calibri",
  });
  params.forEach(([k, v], i) => {
    const y = 1.5 + i * 0.48;
    s.addShape(pres.shapes.LINE, { x: 5.1, y: y - 0.05, w: 4.3, h: 0, line: { color: C.border, width: 0.5 } });
    s.addText(k + ":", { x: 5.15, y, w: 2.0, h: 0.38, fontSize: 11.5, bold: true, color: C.subtext, fontFace: "Calibri" });
    s.addText(v, { x: 7.15, y, w: 2.35, h: 0.38, fontSize: 11.5, color: C.text, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 10 — Hybrid Ensemble & Multi-Modal Design
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Hybrid Ensemble & Multi-Modal Architecture");

  // Ensemble diagram
  addCard(s, 0.45, 1.05, 9.1, 4.4, C.white);

  // Input box
  addCard(s, 0.7, 1.3, 2.0, 0.7, C.darkGreen);
  s.addText("Tea Image\nInput", { x: 0.7, y: 1.3, w: 2.0, h: 0.7, fontSize: 12, bold: true, color: C.white, fontFace: "Calibri", align: "center", valign: "middle" });

  // Lines to models
  s.addShape(pres.shapes.LINE, { x: 2.72, y: 1.65, w: 0.65, h: 0, line: { color: C.accentGreen, width: 1.5 } });
  s.addShape(pres.shapes.LINE, { x: 2.72, y: 1.65, w: 0.4, h: -0.8, line: { color: C.accentGreen, width: 1.5 } });
  s.addShape(pres.shapes.LINE, { x: 2.72, y: 1.65, w: 0.4, h: 0.8, line: { color: C.accentGreen, width: 1.5 } });

  // Model 1
  addCard(s, 3.37, 0.88, 2.2, 0.78, C.lightGreen);
  addAccentBar(s, 3.37, 0.88, 0.78, C.darkGreen);
  s.addText("EfficientNetV2-S", { x: 3.55, y: 0.95, w: 1.95, h: 0.28, fontSize: 11.5, bold: true, color: C.darkGreen, fontFace: "Calibri" });
  s.addText("Acc: 96.48%", { x: 3.55, y: 1.23, w: 1.95, h: 0.28, fontSize: 10.5, color: C.subtext, fontFace: "Calibri" });

  // Model 2
  addCard(s, 3.37, 1.78, 2.2, 0.78, C.lightTeal);
  addAccentBar(s, 3.37, 1.78, 0.78, C.teal);
  s.addText("MobileNetV3Small", { x: 3.55, y: 1.85, w: 1.95, h: 0.28, fontSize: 11.5, bold: true, color: C.teal, fontFace: "Calibri" });
  s.addText("Acc: 79.91%", { x: 3.55, y: 2.13, w: 1.95, h: 0.28, fontSize: 10.5, color: C.subtext, fontFace: "Calibri" });

  // OCR + Barcode
  addCard(s, 3.37, 2.68, 2.2, 0.78, C.lightGold);
  addAccentBar(s, 3.37, 2.68, 0.78, C.gold);
  s.addText("OCR + Barcode", { x: 3.55, y: 2.75, w: 1.95, h: 0.28, fontSize: 11.5, bold: true, color: "#7A5500", fontFace: "Calibri" });
  s.addText("Text Verification", { x: 3.55, y: 3.03, w: 1.95, h: 0.28, fontSize: 10.5, color: C.subtext, fontFace: "Calibri" });

  // Arrows to fusion
  [1.28, 2.17, 3.07].forEach(y => {
    s.addShape(pres.shapes.LINE, { x: 5.59, y, w: 0.5, h: 0, line: { color: C.accentGreen, width: 1.5 } });
  });

  // Fusion box
  addCard(s, 6.1, 1.55, 1.7, 1.5, C.accentGreen);
  s.addText("Weighted\nFusion\n+\nTop-3", {
    x: 6.1, y: 1.55, w: 1.7, h: 1.5,
    fontSize: 12, bold: true, color: C.white, fontFace: "Calibri", align: "center", valign: "middle",
  });

  // Arrow to output
  s.addShape(pres.shapes.LINE, { x: 7.82, y: 2.3, w: 0.48, h: 0, line: { color: C.accentGreen, width: 1.5 } });

  // Output
  addCard(s, 8.3, 1.88, 1.1, 0.85, C.darkGreen);
  s.addText("Nutritional\nAnalytics", { x: 8.3, y: 1.88, w: 1.1, h: 0.85, fontSize: 10.5, bold: true, color: C.white, fontFace: "Calibri", align: "center", valign: "middle" });

  // Bottom detail row
  const details = [
    ["Confidence-Aware", "Top-3 predictions shown with confidence scores — user confirms or searches manually."],
    ["Human-in-the-Loop", "\"None of These? Search\" feature provides graceful recovery from uncertain predictions."],
    ["Temporal Voting", "Multi-frame camera input is combined with temporal voting for stable real-time results."],
  ];

  details.forEach((d, i) => {
    const x = 0.65 + i * 3.05;
    addCard(s, x, 3.65, 2.8, 1.55, C.lightGreen);
    s.addText(d[0], { x: x + 0.12, y: 3.73, w: 2.56, h: 0.32, fontSize: 12, bold: true, color: C.darkGreen, fontFace: "Calibri" });
    s.addText(d[1], { x: x + 0.12, y: 4.08, w: 2.56, h: 1.0, fontSize: 10.5, color: C.subtext, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 11 — Evaluation Metrics
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Evaluation Metrics");

  const metrics = [
    { name: "Accuracy", formula: "TP + TN / Total", desc: "Overall proportion of correct predictions. Baseline comparison metric across all models.", color: C.darkGreen },
    { name: "Precision", formula: "TP / (TP + FP)", desc: "Fraction of positive predictions that are truly positive. Important for reducing false alarms.", color: C.accentGreen },
    { name: "Recall", formula: "TP / (TP + FN)", desc: "Fraction of actual positives correctly identified. Critical for medical-style recall tasks.", color: C.teal },
    { name: "F1-Score (Macro)", formula: "2 × (P × R) / (P + R)", desc: "Harmonic mean of precision and recall. Key metric for imbalanced multi-class problems.", color: C.gold },
    { name: "Confusion Matrix", formula: "Class-wise TP/FP/FN/TN", desc: "Visual analysis of per-class prediction behavior; reveals fine-grained vs easy categories.", color: C.accentGreen },
    { name: "Training Curves", formula: "Loss & Acc over Epochs", desc: "Monitors convergence, early stopping, and overfitting across training and validation sets.", color: C.teal },
  ];

  metrics.forEach((m, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.45 + col * 4.85;
    const y = 1.1 + row * 1.38;
    addCard(s, x, y, 4.5, 1.22, C.white);
    // top color bar
    s.addShape(pres.shapes.RECTANGLE, { x, y, w: 4.5, h: 0.38, fill: { color: m.color }, line: { color: m.color } });
    s.addText(m.name, { x: x + 0.12, y: y + 0.04, w: 2.8, h: 0.3, fontSize: 13, bold: true, color: C.white, fontFace: "Calibri" });
    s.addText(m.formula, { x: x + 0.12, y: y + 0.44, w: 4.2, h: 0.3, fontSize: 11, bold: true, color: C.darkGreen, fontFace: "Calibri", italic: true });
    s.addText(m.desc, { x: x + 0.12, y: y + 0.72, w: 4.2, h: 0.42, fontSize: 10.5, color: C.subtext, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 12 — Results: Model Comparison
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Experimental Results: Model Comparison");

  // Grouped bar chart
  s.addChart(pres.charts.BAR, [
    { name: "Accuracy (%)", labels: ["MobileNetV3Small", "EfficientNetV2-S", "Hybrid Ensemble"], values: [79.91, 96.48, 96.02] },
    { name: "Precision", labels: ["MobileNetV3Small", "EfficientNetV2-S", "Hybrid Ensemble"], values: [81.03, 96.52, 96.25] },
    { name: "Recall", labels: ["MobileNetV3Small", "EfficientNetV2-S", "Hybrid Ensemble"], values: [79.91, 96.48, 96.02] },
    { name: "F1-Score (%)", labels: ["MobileNetV3Small", "EfficientNetV2-S", "Hybrid Ensemble"], values: [79.80, 96.47, 96.03] },
  ], {
    x: 0.45, y: 1.0, w: 6.0, h: 4.5,
    barDir: "col", barGrouping: "clustered",
    chartColors: ["43A047", "1B5E20", "00796B", "F9A825"],
    chartArea: { fill: { color: C.white } },
    catAxisLabelColor: C.subtext,
    valAxisLabelColor: C.subtext,
    valAxisMinVal: 70,
    valAxisMaxVal: 100,
    valGridLine: { color: "E8F5E9", size: 0.5 },
    catGridLine: { style: "none" },
    showLegend: true,
    legendPos: "b",
    legendFontSize: 10,
    showTitle: true,
    title: "Performance Metrics by Model",
    titleColor: C.darkGreen,
    titleFontSize: 12,
  });

  // Table right side
  const rows = [
    [{ text: "Model", options: { bold: true, fill: { color: C.darkGreen }, color: C.white, fontSize: 11 } },
     { text: "Acc %", options: { bold: true, fill: { color: C.darkGreen }, color: C.white, fontSize: 11 } },
     { text: "F1", options: { bold: true, fill: { color: C.darkGreen }, color: C.white, fontSize: 11 } }],
    ["MobileNetV3Small", "79.91", "0.7980"],
    [{ text: "EfficientNetV2-S", options: { bold: true, fill: { color: C.lightGreen } } }, { text: "96.48 ★", options: { bold: true, fill: { color: C.lightGreen } } }, { text: "0.9647 ★", options: { bold: true, fill: { color: C.lightGreen } } }],
    ["Hybrid Ensemble", "96.02", "0.9603"],
  ];

  s.addTable(rows, {
    x: 6.8, y: 1.3, w: 2.75, colW: [1.45, 0.7, 0.65],
    border: { pt: 1, color: C.border },
    fill: { color: C.white },
    fontSize: 11,
    align: "center",
    fontFace: "Calibri",
  });

  // EfficientNet highlight box
  addCard(s, 6.8, 3.0, 2.75, 1.0, C.lightGreen);
  addAccentBar(s, 6.8, 3.0, 1.0, C.darkGreen);
  s.addText("Best Model: EfficientNetV2-S", { x: 7.0, y: 3.08, w: 2.45, h: 0.3, fontSize: 12, bold: true, color: C.darkGreen, fontFace: "Calibri" });
  s.addText("96.48% accuracy\nMacro F1: 0.9647\n18-class classification", { x: 7.0, y: 3.42, w: 2.45, h: 0.52, fontSize: 10.5, color: C.subtext, fontFace: "Calibri" });

  // Training highlights
  addCard(s, 6.8, 4.1, 2.75, 1.15, C.lightTeal);
  s.addText("Training: 15 epochs, converged at E12\nMin. overfitting — strong generalization\nDataset: 4,320 train / 1,080 val images", {
    x: 6.95, y: 4.18, w: 2.5, h: 1.0,
    fontSize: 10.5, color: C.text, fontFace: "Calibri",
  });
}

// ─────────────────────────────────────────────
// SLIDE 13 — Training Performance Curves
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Training Performance: EfficientNetV2-S");

  const epochs = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15];
  const trainAcc = [52.1,68.4,77.3,83.1,87.5,90.2,91.8,93.4,94.2,95.1,95.7,96.1,96.3,96.4,96.5];
  const valAcc =   [48.3,63.7,73.9,80.4,84.8,88.0,90.2,92.1,93.1,94.2,94.9,95.6,96.0,96.3,96.4];
  const trainLoss= [1.82,1.31,0.98,0.75,0.58,0.46,0.38,0.31,0.26,0.22,0.19,0.16,0.14,0.13,0.12];
  const valLoss =  [1.95,1.44,1.08,0.82,0.63,0.50,0.41,0.34,0.28,0.24,0.21,0.18,0.15,0.13,0.12];

  s.addChart(pres.charts.LINE, [
    { name: "Train Accuracy", labels: epochs.map(String), values: trainAcc },
    { name: "Val Accuracy", labels: epochs.map(String), values: valAcc },
  ], {
    x: 0.45, y: 1.0, w: 4.7, h: 4.3,
    chartColors: ["2E7D32", "43A047"],
    lineSize: 2.5, lineSmooth: true,
    chartArea: { fill: { color: C.white } },
    catAxisLabelColor: C.subtext,
    valAxisLabelColor: C.subtext,
    valAxisMinVal: 45,
    valAxisMaxVal: 100,
    valGridLine: { color: "E8F5E9", size: 0.5 },
    showLegend: true, legendPos: "b", legendFontSize: 10,
    showTitle: true, title: "Accuracy over Epochs (%)", titleColor: C.darkGreen, titleFontSize: 12,
  });

  s.addChart(pres.charts.LINE, [
    { name: "Train Loss", labels: epochs.map(String), values: trainLoss },
    { name: "Val Loss", labels: epochs.map(String), values: valLoss },
  ], {
    x: 5.3, y: 1.0, w: 4.3, h: 4.3,
    chartColors: ["2E7D32", "F9A825"],
    lineSize: 2.5, lineSmooth: true,
    chartArea: { fill: { color: C.white } },
    catAxisLabelColor: C.subtext,
    valAxisLabelColor: C.subtext,
    valGridLine: { color: "E8F5E9", size: 0.5 },
    showLegend: true, legendPos: "b", legendFontSize: 10,
    showTitle: true, title: "Loss over Epochs", titleColor: C.darkGreen, titleFontSize: 12,
  });

  s.addText("Model converged rapidly with minimal gap between train/validation curves, confirming strong generalization with no significant overfitting.", {
    x: 0.45, y: 5.32, w: 9.1, h: 0.25,
    fontSize: 11, italic: true, color: C.muted, fontFace: "Calibri", align: "center",
  });
}

// ─────────────────────────────────────────────
// SLIDE 14 — Discussion of Results
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Discussion of Findings");

  const findings = [
    { head: "EfficientNetV2-S Outperforms", body: "96.48% accuracy and 0.9647 macro F1 across 18 classes. Superior compound scaling extracts fine-grained tea features effectively.", color: C.darkGreen },
    { head: "Hybrid Ensemble Close But Not Superior", body: "Ensemble (96.02%) marginally trails EfficientNetV2-S, suggesting the base model already learned highly discriminative representations.", color: C.accentGreen },
    { head: "MobileNetV3Small: Speed-Accuracy Tradeoff", body: "79.91% accuracy but significantly lower compute cost — ideal for battery-constrained or offline-only deployment scenarios.", color: C.teal },
    { head: "OCR + Barcode Boost Robustness", body: "When visual confidence is low, text and barcode channels provide complementary verification, especially for packaged products.", color: C.gold },
    { head: "Confidence-Aware Top-3 Mechanism", body: "Presenting 3 options with confidence scores significantly improves user trust and catches edge cases where the top-1 prediction fails.", color: C.accentGreen },
    { head: "Nutritional Analytics Add Real Value", body: "Beyond classification, the integrated knowledge base transforms the app into a personal wellness assistant with scan history and dashboards.", color: C.darkGreen },
  ];

  findings.forEach((f, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.45 + col * 4.85;
    const y = 1.08 + row * 1.45;
    addCard(s, x, y, 4.5, 1.28, C.white);
    addAccentBar(s, x, y, 1.28, f.color);
    s.addText(f.head, { x: x + 0.22, y: y + 0.1, w: 4.16, h: 0.32, fontSize: 12.5, bold: true, color: C.darkGreen, fontFace: "Calibri" });
    s.addText(f.body, { x: x + 0.22, y: y + 0.44, w: 4.16, h: 0.74, fontSize: 11, color: C.subtext, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 15 — Mobile Deployment & App Screenshots
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Mobile Deployment: Android Application");

  const screens = [
    { title: "Scan Interface", desc: "CameraX-powered capture. Real-time frame quality analysis before inference.", color: C.darkGreen },
    { title: "Top-3 Predictions", desc: "Confidence-ranked results with user confirmation and manual search fallback.", color: C.teal },
    { title: "Nutrition Profile", desc: "Calories, caffeine, antioxidants, minerals, and health benefits per tea.", color: C.accentGreen },
    { title: "Analytics Dashboard", desc: "Scan history, usage trends, category breakdown using MPAndroidChart.", color: C.gold },
  ];

  screens.forEach((sc, i) => {
    const x = 0.45 + i * 2.35;
    // Phone frame
    addCard(s, x, 1.05, 2.1, 3.0, C.white);
    s.addShape(pres.shapes.RECTANGLE, {
      x: x + 0.1, y: 1.15, w: 1.9, h: 2.35,
      fill: { color: i % 2 === 0 ? C.lightGreen : C.lightTeal },
      line: { color: C.border, width: 0.5 },
    });
    // Screen label with icon
    s.addText(["📷","🍃","📊","📈"][i], {
      x: x + 0.1, y: 1.22, w: 1.9, h: 0.8,
      fontSize: 36, align: "center", valign: "middle",
    });
    s.addText(sc.title, {
      x: x + 0.1, y: 2.08, w: 1.9, h: 0.35,
      fontSize: 11, bold: true, color: sc.color, fontFace: "Calibri", align: "center",
    });
    s.addText(sc.desc, {
      x: x + 0.1, y: 2.42, w: 1.9, h: 1.0,
      fontSize: 9.5, color: C.subtext, fontFace: "Calibri", align: "center",
    });
  });

  // Tech stack
  addCard(s, 0.45, 4.28, 9.1, 1.2, C.lightGreen);
  s.addText("Technology Stack:", { x: 0.62, y: 4.38, w: 1.6, h: 0.3, fontSize: 12, bold: true, color: C.darkGreen, fontFace: "Calibri" });
  const techs = ["TensorFlow Lite", "CameraX", "Google ML Kit", "Kotlin", "Room DB", "MPAndroidChart", "OpenFoodFacts API"];
  techs.forEach((t, i) => {
    addCard(s, 0.62 + i * 1.3, 4.72, 1.2, 0.52, C.white);
    s.addText(t, { x: 0.62 + i * 1.3, y: 4.72, w: 1.2, h: 0.52, fontSize: 9.5, color: C.darkGreen, fontFace: "Calibri", align: "center", valign: "middle", bold: true });
  });
}

// ─────────────────────────────────────────────
// SLIDE 16 — Future Work
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Future Work");

  const futures = [
    { num: "01", head: "Dataset Expansion", body: "Grow to 50+ tea varieties including international brands, specialty blends, and medicinal herbal infusions with diverse conditions.", color: C.darkGreen },
    { num: "02", head: "Real Quantum Hardware Experiments", body: "Execute quantum kernel SVM on actual quantum processors for comparison against classical multi-modal AI.", color: C.accentGreen },
    { num: "03", head: "Explainable AI (Grad-CAM)", body: "Implement saliency maps and Grad-CAM to highlight prediction-driving image regions for transparency and user trust.", color: C.teal },
    { num: "04", head: "Personalized Recommendations", body: "Analyze scan history and health goals to suggest teas for stress, sleep, digestion, or weight management.", color: C.gold },
    { num: "05", head: "Continual Learning", body: "Use user corrections from \"None of These?\" to incrementally retrain the model, improving over time automatically.", color: C.accentGreen },
    { num: "06", head: "Cloud Integration & IoT", body: "Sync analytics to cloud; integrate with smart kitchen devices for automated tea preparation and dietary tracking.", color: C.teal },
  ];

  futures.forEach((f, i) => {
    const col = i % 3;
    const row = Math.floor(i / 3);
    const x = 0.45 + col * 3.17;
    const y = 1.1 + row * 2.18;
    addCard(s, x, y, 2.9, 1.95, C.white);
    s.addShape(pres.shapes.OVAL, { x: x + 1.15, y: y - 0.28, w: 0.58, h: 0.58, fill: { color: f.color }, line: { color: f.color } });
    s.addText(f.num, { x: x + 1.15, y: y - 0.28, w: 0.58, h: 0.58, fontSize: 13, bold: true, color: C.white, fontFace: "Calibri", align: "center", valign: "middle" });
    s.addText(f.head, { x: x + 0.12, y: y + 0.38, w: 2.66, h: 0.38, fontSize: 12, bold: true, color: C.darkGreen, fontFace: "Calibri", align: "center" });
    s.addText(f.body, { x: x + 0.12, y: y + 0.78, w: 2.66, h: 1.05, fontSize: 10.5, color: C.subtext, fontFace: "Calibri", align: "center" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 17 — Conclusion
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.offWhite);
  addTitle(s, "Conclusion");

  const takeaways = [
    ["96.48% Accuracy", "EfficientNetV2-S with transfer learning achieves 96.48% accuracy and 0.9647 macro F1-score across 18 tea categories.", C.darkGreen],
    ["Multi-Modal Robustness", "Combining image AI with OCR and barcode verification improves recognition reliability in real-world packaging scenarios.", C.teal],
    ["Confidence-Aware UX", "Top-3 predictions with user confirmation and manual fallback significantly improve practical usability and system trustworthiness.", C.accentGreen],
    ["Deployed on Android", "TFLite-powered Kotlin app supports real-time scanning, nutritional analytics, scan history, and personalized wellness insights.", C.gold],
    ["Nutritional Intelligence", "Integrated knowledge base covering calories, caffeine, antioxidants, and health benefits transforms the app into a wellness platform.", C.teal],
    ["Foundation for AI Wellness", "Framework demonstrates that multi-modal AI with mobile deployment can power scalable, accurate food recognition for personalized nutrition.", C.darkGreen],
  ];

  takeaways.forEach(([head, body, color], i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = 0.45 + col * 4.85;
    const y = 1.08 + row * 1.42;
    addCard(s, x, y, 4.5, 1.25, C.white);
    s.addShape(pres.shapes.RECTANGLE, { x, y, w: 4.5, h: 0.42, fill: { color: color }, line: { color } });
    s.addText(head, { x: x + 0.12, y: y + 0.05, w: 4.26, h: 0.32, fontSize: 13, bold: true, color: C.white, fontFace: "Calibri" });
    s.addText(body, { x: x + 0.12, y: y + 0.5, w: 4.26, h: 0.66, fontSize: 11, color: C.subtext, fontFace: "Calibri" });
  });
}

// ─────────────────────────────────────────────
// SLIDE 18 — Thank You
// ─────────────────────────────────────────────
{
  const s = pres.addSlide();
  setBg(s, C.darkGreen);

  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: 0.25, h: 5.625, fill: { color: C.gold }, line: { color: C.gold } });
  s.addShape(pres.shapes.RECTANGLE, { x: 0.25, y: 4.8, w: 9.75, h: 0.82, fill: { color: "1B5E20" }, line: { color: "1B5E20" } });

  s.addText("🍵", { x: 3.5, y: 0.55, w: 3.0, h: 2.0, fontSize: 72, align: "center", valign: "middle" });

  s.addText("Thank You", {
    x: 0.5, y: 2.5, w: 9.0, h: 1.1,
    fontSize: 52, bold: true, color: C.white, fontFace: "Calibri", align: "center",
  });

  s.addText("Any Questions?", {
    x: 0.5, y: 3.55, w: 9.0, h: 0.48,
    fontSize: 20, italic: true, color: C.midGreen, fontFace: "Calibri", align: "center",
  });

  s.addShape(pres.shapes.LINE, { x: 2.0, y: 4.12, w: 6.0, h: 0, line: { color: C.midGreen, width: 1 } });

  s.addText("A Multi-Modal AI Framework for Real-Time Tea Recognition and Personalized Nutritional Analytics", {
    x: 0.5, y: 4.22, w: 9.0, h: 0.45,
    fontSize: 11, italic: true, color: C.muted, fontFace: "Calibri", align: "center",
  });

  s.addText("Deepti Joshi  •  Amity University Haryana  •  M.Tech AI  •  2026", {
    x: 0.5, y: 4.88, w: 9.0, h: 0.3,
    fontSize: 11, color: "1B5E20", fontFace: "Calibri", align: "center",
  });
}

pres.writeFile({ fileName: "/mnt/user-data/outputs/Tea_AI_Framework.pptx" })
  .then(() => console.log("✅ Presentation saved to /mnt/user-data/outputs/Tea_AI_Framework.pptx"))
  .catch(err => { console.error("Error:", err); process.exit(1); });