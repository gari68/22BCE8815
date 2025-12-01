WITH emp_total AS (
  SELECT
    e.emp_id,
    e.first_name,
    e.last_name,
    e.dob,
    e.department,
    d.department_name,
    COALESCE(
      SUM(
        CASE
          WHEN EXTRACT(DAY FROM p.payment_time) <> 1 THEN p.amount
          ELSE 0
        END
      ), 0
    ) AS salary
  FROM employee e
  LEFT JOIN payments p ON e.emp_id = p.emp_id
  LEFT JOIN department d ON e.department = d.department_id
  GROUP BY e.emp_id, e.first_name, e.last_name, e.dob, e.department, d.department_name
)
SELECT
  department_name AS department_name,
  salary AS salary,
  first_name || ' ' || last_name AS employee_name,
  (DATE_PART('year', AGE('2025-12-01'::date, dob)))::int AS age
FROM (
  SELECT et.*,
         ROW_NUMBER() OVER (PARTITION BY et.department ORDER BY et.salary DESC, et.emp_id) AS rn
  FROM emp_total et
) t
WHERE rn = 1
ORDER BY department_name;
