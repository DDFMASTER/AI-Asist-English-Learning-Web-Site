/**
 * 英文词形还原（Lemmatization）工具
 *
 * 将屈折变化的单词还原为词典原形：
 *   名词复数 → 单数    children → child
 *   动词过去/分词 → 原形  ran → run, running → run
 *   形容词比较级 → 原级  better → good
 *   副词 → 形容词      quickly → quick
 *
 * 策略：不规则词表优先 → 规则后缀剥离
 */

// ==================== 不规则词表 ====================

const IRREGULAR = {
  // ── 动词 ──
  was: 'be', were: 'be', been: 'be', being: 'be',
  'won\'t': 'will', wouldn: 'will',
  went: 'go', gone: 'go', goes: 'go',
  ran: 'run', runs: 'run', running: 'run',
  saw: 'see', seen: 'see', sees: 'see',
  did: 'do', done: 'do', does: 'do',
  had: 'have', has: 'have', having: 'have',
  made: 'make', makes: 'make', making: 'make',
  took: 'take', takes: 'take', taken: 'take', taking: 'take',
  came: 'come', comes: 'come', coming: 'come',
  gave: 'give', gives: 'give', given: 'give', giving: 'give',
  got: 'get', gets: 'get', gotten: 'get', getting: 'get',
  knew: 'know', knows: 'know', known: 'know',
  thought: 'think', thinks: 'think', thinking: 'think',
  told: 'tell', tells: 'tell', telling: 'tell',
  said: 'say', says: 'say', saying: 'say',
  put: 'put', puts: 'put', putting: 'put',
  wrote: 'write', writes: 'write', written: 'write', writing: 'write',
  spoke: 'speak', speaks: 'speak', spoken: 'speak', speaking: 'speak',
  drove: 'drive', drives: 'drive', driven: 'drive', driving: 'drive',
  ate: 'eat', eats: 'eat', eaten: 'eat', eating: 'eat',
  flew: 'fly', flies: 'fly', flown: 'fly', flying: 'fly',
  grew: 'grow', grows: 'grow', grown: 'grow',
  fell: 'fall', falls: 'fall', fallen: 'fall',
  felt: 'feel', feels: 'feel', feeling: 'feel',
  found: 'find', finds: 'find', finding: 'find',
  bought: 'buy', buys: 'buy', buying: 'buy',
  brought: 'bring', brings: 'bring', bringing: 'bring',
  caught: 'catch', catches: 'catch', catching: 'catch',
  taught: 'teach', teaches: 'teach', teaching: 'teach',
  fought: 'fight', fights: 'fight', fighting: 'fight',
  built: 'build', builds: 'build', building: 'build',
  sent: 'send', sends: 'send', sending: 'send',
  spent: 'spend', spends: 'spend', spending: 'spend',
  kept: 'keep', keeps: 'keep', keeping: 'keep',
  slept: 'sleep', sleeps: 'sleep', sleeping: 'sleep',
  left: 'leave', leaves: 'leave', leaving: 'leave',
  lost: 'lose', loses: 'lose', losing: 'lose',
  won: 'win', wins: 'win', winning: 'win',
  met: 'meet', meets: 'meet', meeting: 'meet',
  sat: 'sit', sits: 'sit', sitting: 'sit',
  stood: 'stand', stands: 'stand', standing: 'stand',
  understood: 'understand', understands: 'understand',
  began: 'begin', begins: 'begin', begun: 'begin', beginning: 'begin',
  drank: 'drink', drinks: 'drink', drunk: 'drink', drinking: 'drink',
  sang: 'sing', sings: 'sing', sung: 'sing', singing: 'sing',
  rang: 'ring', rings: 'ring', rung: 'ring',
  swam: 'swim', swims: 'swim', swum: 'swim', swimming: 'swim',
  chose: 'choose', chooses: 'choose', chosen: 'choose', choosing: 'choose',
  broke: 'break', breaks: 'break', broken: 'break', breaking: 'break',
  woke: 'wake', wakes: 'wake', woken: 'wake', waking: 'wake',
  rose: 'rise', rises: 'rise', risen: 'rise', rising: 'rise',
  rode: 'ride', rides: 'ride', ridden: 'ride', riding: 'ride',
  threw: 'throw', throws: 'throw', thrown: 'throw', throwing: 'throw',
  drew: 'draw', draws: 'draw', drawn: 'draw', drawing: 'draw',
  blew: 'blow', blows: 'blow', blown: 'blow', blowing: 'blow',
  paid: 'pay', pays: 'pay', paying: 'pay',
  led: 'lead', leads: 'lead', leading: 'lead',
  meant: 'mean', means: 'mean', meaning: 'mean',
  held: 'hold', holds: 'hold', holding: 'hold',
  became: 'become', becomes: 'become', becoming: 'become',
  showed: 'show', shows: 'show', shown: 'show', showing: 'show',
  wore: 'wear', wears: 'wear', worn: 'wear', wearing: 'wear',
  bore: 'bear', bears: 'bear', born: 'bear', borne: 'bear', bearing: 'bear',
  sought: 'seek', seeks: 'seek', seeking: 'seek',
  struck: 'strike', strikes: 'strike', striking: 'strike',
  shut: 'shut', shuts: 'shut', shutting: 'shut',
  cost: 'cost', costs: 'cost', costing: 'cost',
  hit: 'hit', hits: 'hit', hitting: 'hit',
  let: 'let', lets: 'let', letting: 'let',
  set: 'set', sets: 'set', setting: 'set',
  spread: 'spread', spreads: 'spread', spreading: 'spread',

  // ── 名词 ──
  children: 'child',
  men: 'man', women: 'woman',
  mice: 'mouse',
  feet: 'foot',
  teeth: 'tooth',
  geese: 'goose',
  people: 'person', persons: 'person',
  criteria: 'criterion',
  phenomena: 'phenomenon',
  data: 'datum',
  alumni: 'alumnus',
  indices: 'index', indexes: 'index',
  analyses: 'analysis',
  theses: 'thesis',
  bases: 'basis',
  crises: 'crisis',
  axes: 'axis',
  diagnoses: 'diagnosis',
  hypotheses: 'hypothesis',
  parentheses: 'parenthesis',
  oxen: 'ox',
  lives: 'life',
  shelves: 'shelf',
  selves: 'self',
  wolves: 'wolf',
  thieves: 'thief',
  loaves: 'loaf',
  halves: 'half',
  knives: 'knife',
  leaves: 'leaf',
  wives: 'wife',
  elves: 'elf',
  calves: 'calf',
  scarves: 'scarf',
  hooves: 'hoof',
  dwarves: 'dwarf',
  // 拉丁/希腊词源复数
  cacti: 'cactus',
  fungi: 'fungus',
  stimuli: 'stimulus',
  syllabi: 'syllabus',
  foci: 'focus',
  radii: 'radius',
  bacteria: 'bacterium',
  curricula: 'curriculum',
  media: 'medium',
  memoranda: 'memorandum',
  strata: 'stratum',
  vertebrae: 'vertebra',
  formulae: 'formula',
  antennae: 'antenna',
  millennia: 'millennium',
  automata: 'automaton',
  errata: 'erratum',
  addenda: 'addendum',
  oases: 'oasis',
  neuroses: 'neurosis',
  ellipses: 'ellipsis',
  synopses: 'synopsis',
  // 其他常见不规则复数
  corps: 'corps',
  series: 'series',
  species: 'species',
  means: 'means',
  offspring: 'offspring',
  sheep: 'sheep',
  deer: 'deer',
  fish: 'fish',
  aircraft: 'aircraft',

  // ── 形容词 / 副词 ──
  better: 'good', best: 'good',
  worse: 'bad', worst: 'bad',
  more: 'many', most: 'many',
  less: 'little', least: 'little',
  farther: 'far', farthest: 'far', further: 'far', furthest: 'far',
  older: 'old', oldest: 'old', elder: 'old', eldest: 'old',
  // 不规则比较级/最高级
  later: 'late', latest: 'late', latter: 'late', last: 'late',
  bigger: 'big', biggest: 'big',
  larger: 'large', largest: 'large',
  smaller: 'small', smallest: 'small',
  higher: 'high', highest: 'high',
  lower: 'low', lowest: 'low',
  younger: 'young', youngest: 'young',
  longer: 'long', longest: 'long',
  shorter: 'short', shortest: 'short',
  faster: 'fast', fastest: 'fast',
  stronger: 'strong', strongest: 'strong',
  weaker: 'weak', weakest: 'weak',
  richer: 'rich', richest: 'rich',
  poorer: 'poor', poorest: 'poor',
  wider: 'wide', widest: 'wide',
  deeper: 'deep', deepest: 'deep',
  heavier: 'heavy', heaviest: 'heavy',
  busier: 'busy', busiest: 'busy',
  funnier: 'funny', funniest: 'funny',
  luckier: 'lucky', luckiest: 'lucky',
  lovelier: 'lovely', loveliest: 'lovely',
  angrier: 'angry', angriest: 'angry',
  dirtier: 'dirty', dirtiest: 'dirty',
  drier: 'dry', driest: 'dry',
  earlier: 'early', earliest: 'early',
  uglier: 'ugly', ugliest: 'ugly',
  tidier: 'tidy', tidiest: 'tidy',
  healthier: 'healthy', healthiest: 'healthy',
  wealthier: 'wealthy', wealthiest: 'wealthy',
  noisier: 'noisy', noisiest: 'noisy',
  sunnier: 'sunny', sunniest: 'sunny',
  windier: 'windy', windiest: 'windy',
  friendlier: 'friendly', friendliest: 'friendly',
  livelier: 'lively', liveliest: 'lively',
  happier: 'happy', happiest: 'happy',
  easier: 'easy', easiest: 'easy',
  prettier: 'pretty', prettiest: 'pretty',
}

// 一词多源补充：某些变形对应多个原形（如 better → good/well）
const SECONDARY_IRREGULAR = {
  better: 'well', best: 'well',
  worse: 'ill',  // worse 也可以是 ill 的比较级
  // more/most 的 "much" 义项已在主表中用 "many"
}

// ==================== 规则还原 ====================

/**
 * 根据后缀规则生成候选原形列表
 * 按置信度从高到低排列
 */
function ruleBasedCandidates(word) {
  const candidates = []

  // 1. -ies → -y  (studies→study, policies→policy, carries→carry)
  if (word.endsWith('ies') && word.length > 4) {
    candidates.push(word.slice(0, -3) + 'y')
  }

  // 2. -ves → -f / -fe  (wolves→wolf, knives→knife, lives→life)
  if (word.endsWith('ves') && word.length > 4) {
    candidates.push(word.slice(0, -3) + 'f')
    candidates.push(word.slice(0, -3) + 'fe')
  }

  // 3. -es → remove  (watches→watch, boxes→box, buses→bus)
  //    仅当词干以 s, sh, ch, x, z, o 结尾时适用
  if (word.endsWith('es') && word.length > 4) {
    const stem = word.slice(0, -2)
    if (stem.endsWith('s') || stem.endsWith('sh') || stem.endsWith('ch') ||
        stem.endsWith('x') || stem.endsWith('z') || stem.endsWith('o')) {
      candidates.push(stem)
    }
    // 也尝试直接去 -s（针对 -e 结尾的词，如 creates→create）
    candidates.push(stem + 'e')
  }

  // 4. -s → remove  (regular plural / 3rd person singular)
  if (word.endsWith('s') && !word.endsWith('ss') && word.length > 3) {
    candidates.push(word.slice(0, -1))
  }

  // 5. -ied → -y  (studied→study, carried→carry)
  if (word.endsWith('ied') && word.length > 4) {
    candidates.push(word.slice(0, -3) + 'y')
  }

  // 6. -ed → remove / -d → -e  (walked→walk, created→create, liked→like)
  if (word.endsWith('ed') && word.length > 4) {
    const stem = word.slice(0, -2)
    candidates.push(stem)          // walked → walk
    candidates.push(stem + 'e')    // created → create (but stem is "creat")
    // 双写辅音还原：stopped → stop
    if (stem.length >= 3) {
      const last = stem[stem.length - 1]
      const secondLast = stem[stem.length - 2]
      if (last === secondLast && isConsonant(last)) {
        candidates.push(stem.slice(0, -1))  // stopped → stop
      }
    }
  }

  // 7. -ing → 多种还原
  if (word.endsWith('ing') && word.length > 5) {
    const stem = word.slice(0, -3)
    candidates.push(stem)          // walking → walk, doing → do
    candidates.push(stem + 'e')    // creating → create (stem = "creat")
    // 双写辅音还原：running → run, sitting → sit
    if (stem.length >= 3) {
      const last = stem[stem.length - 1]
      const secondLast = stem[stem.length - 2]
      if (last === secondLast && isConsonant(last)) {
        candidates.push(stem.slice(0, -1))  // running → run
      }
    }
  }

  // 8. -ier → -y  (happier→happy, easier→easy, prettier→pretty)
  if (word.endsWith('ier') && word.length > 4) {
    candidates.push(word.slice(0, -3) + 'y')
  }

  // 9. -iest → -y  (happiest→happy, easiest→easy, prettiest→pretty)
  if (word.endsWith('iest') && word.length > 5) {
    candidates.push(word.slice(0, -4) + 'y')
  }

  // 10. -er → remove / -e  (bigger→big, later→late, teacher→teach)
  if (word.endsWith('er') && !word.endsWith('ier') && word.length > 4) {
    const stem = word.slice(0, -2)
    candidates.push(stem)
    candidates.push(stem + 'e')
    // 双写辅音：bigger→big
    if (stem.length >= 3) {
      const last = stem[stem.length - 1]
      const secondLast = stem[stem.length - 2]
      if (last === secondLast && isConsonant(last)) {
        candidates.push(stem.slice(0, -1))
      }
    }
    // -ier 已被规则8处理
  }

  // 11. -est → remove / -e  (biggest→big, latest→late)
  if (word.endsWith('est') && !word.endsWith('iest') && word.length > 5) {
    const stem = word.slice(0, -3)
    candidates.push(stem)
    candidates.push(stem + 'e')
    // 双写辅音：biggest→big
    if (stem.length >= 3) {
      const last = stem[stem.length - 1]
      const secondLast = stem[stem.length - 2]
      if (last === secondLast && isConsonant(last)) {
        candidates.push(stem.slice(0, -1))
      }
    }
    // -iest 已被规则9处理
  }

  // 12. -ly → remove  (quickly→quick, happily→happy)
  if (word.endsWith('ly') && word.length > 4) {
    const stem = word.slice(0, -2)
    candidates.push(stem)
    // -ily → -y  (happily→happy)
    if (stem.endsWith('i')) {
      candidates.push(stem.slice(0, -1) + 'y')
    }
  }

  return candidates
}

function isConsonant(ch) {
  return /[bcdfghjklmnpqrstvwxyz]/i.test(ch)
}

// ==================== 主入口 ====================

/**
 * 对单词进行词形还原，返回候选原形列表（按置信度从高到低）
 * @param {string} word - 输入单词
 * @returns {string[]} 候选原形列表（已去重），第一个是原词本身
 */
export function lemmatize(word) {
  if (!word || word.length <= 2) return [word]

  const w = word.toLowerCase()
  const candidates = []

  // 0. 原词（精确匹配优先级最高）
  candidates.push(w)

  // 1. 查不规则词表
  if (IRREGULAR[w]) {
    const base = IRREGULAR[w]
    if (!candidates.includes(base)) {
      candidates.push(base)
    }
    // 一词多源：查是否有次级原形（如 better → good 之后也尝试 well）
    const secondary = SECONDARY_IRREGULAR[w]
    if (secondary && !candidates.includes(secondary)) {
      candidates.push(secondary)
    }
  }

  // 2. 规则还原
  const ruleResults = ruleBasedCandidates(w)
  for (const c of ruleResults) {
    if (!candidates.includes(c) && c.length >= 2) {
      candidates.push(c)
    }
  }

  return candidates
}

/**
 * 对单词进行词形还原，返回唯一最佳原形
 * @param {string} word
 * @returns {string} 最佳猜测原形
 */
export function lemmatizeOne(word) {
  const candidates = lemmatize(word)
  // 返回第一个非原词的候选（即最可能的原形）
  return candidates.length > 1 ? candidates[1] : candidates[0]
}
